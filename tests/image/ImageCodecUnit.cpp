#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <vector>

#include <jpeglib.h>
#include <png.h>
#include <webp/encode.h>

#include "ImageCodec/ImageCodec.hpp"

static int checks = 0;
static int failures = 0;

static void Expect(bool condition, const char* description)
{
    checks += 1;

    if (condition)
    {
        printf("  ok    %s\n", description);
        return;
    }

    printf("  FAIL  %s\n", description);
    failures += 1;
}

static void CollectPngBytes(png_structp png, png_bytep bytes, png_size_t length)
{
    std::vector<uint8_t>* into = static_cast<std::vector<uint8_t>*>(png_get_io_ptr(png));
    into->insert(into->end(), bytes, bytes + length);
}

static void FlushPngBytes(png_structp)
{
}

static bool EncodePng(uint32_t width, uint32_t height, bool withAlpha, std::vector<uint8_t>& into)
{
    png_structp png = png_create_write_struct(PNG_LIBPNG_VER_STRING, nullptr, nullptr, nullptr);
    if (!png) return false;

    png_infop info = png_create_info_struct(png);
    if (!info)
    {
        png_destroy_write_struct(&png, nullptr);
        return false;
    }

    if (setjmp(png_jmpbuf(png)))
    {
        png_destroy_write_struct(&png, &info);
        return false;
    }

    png_set_write_fn(png, &into, &CollectPngBytes, &FlushPngBytes);

    int colorType = withAlpha ? PNG_COLOR_TYPE_RGB_ALPHA : PNG_COLOR_TYPE_RGB;
    int channels = withAlpha ? 4 : 3;

    png_set_IHDR(png, info, width, height, 8, colorType, PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT,
                 PNG_FILTER_TYPE_DEFAULT);
    png_write_info(png, info);

    std::vector<uint8_t> row(static_cast<size_t>(width) * channels);
    std::vector<png_bytep> rows(height);
    std::vector<uint8_t> storage(static_cast<size_t>(width) * height * channels);

    for (uint32_t y = 0; y < height; ++y)
    {
        for (uint32_t x = 0; x < width; ++x)
        {
            size_t offset = (static_cast<size_t>(y) * width + x) * channels;

            storage[offset] = x < width / 2 ? 255 : 0;
            storage[offset + 1] = y < height / 2 ? 255 : 0;
            storage[offset + 2] = 0;

            if (withAlpha) storage[offset + 3] = x < width / 2 ? 255 : 128;
        }

        rows[y] = storage.data() + static_cast<size_t>(y) * width * channels;
    }

    png_write_image(png, rows.data());
    png_write_end(png, nullptr);
    png_destroy_write_struct(&png, &info);

    return true;
}

static uint32_t PixelAt(const ImageCodec::Image& image, uint32_t x, uint32_t y)
{
    const uint8_t* row = image.pixels + static_cast<size_t>(y) * image.stride;
    return reinterpret_cast<const uint32_t*>(row)[x];
}

static void CheckDetection()
{
    std::vector<uint8_t> png;
    EncodePng(8, 8, false, png);

    Expect(ImageCodec::Detect(png.data(), png.size()) == ImageCodec::Format::Png, "a png is recognised by its signature");

    const uint8_t jpegHead[4] = {0xFF, 0xD8, 0xFF, 0xE0};
    Expect(ImageCodec::Detect(jpegHead, sizeof(jpegHead)) == ImageCodec::Format::Jpeg, "a jpeg is recognised by its marker");

    uint8_t webpHead[12];
    std::memcpy(webpHead, "RIFF", 4);
    std::memcpy(webpHead + 4, "\0\0\0\0", 4);
    std::memcpy(webpHead + 8, "WEBP", 4);
    Expect(ImageCodec::Detect(webpHead, sizeof(webpHead)) == ImageCodec::Format::Webp, "a webp is recognised by its container");

    const uint8_t rubbish[8] = {1, 2, 3, 4, 5, 6, 7, 8};
    Expect(ImageCodec::Detect(rubbish, sizeof(rubbish)) == ImageCodec::Format::Unknown, "anything else is not claimed");
    Expect(ImageCodec::Detect(nullptr, 0) == ImageCodec::Format::Unknown, "no bytes at all is not claimed");
}

static void CheckOpaquePng()
{
    std::vector<uint8_t> encoded;
    Expect(EncodePng(16, 16, false, encoded), "the test can write a png to decode");

    ImageCodec::Image image;
    ImageCodec::Result outcome = ImageCodec::Decode(encoded.data(), encoded.size(), image);

    Expect(outcome == ImageCodec::Result::Ok, "an opaque png decodes");
    Expect(image.width == 16 && image.height == 16, "an opaque png keeps its size");
    Expect(!image.hasAlpha, "an opaque png is reported opaque");

    uint32_t topLeft = PixelAt(image, 2, 2);
    uint32_t topRight = PixelAt(image, 12, 2);
    uint32_t bottomLeft = PixelAt(image, 2, 12);

    Expect(((topLeft >> 16) & 0xFF) == 255, "red lands in the red channel, not the blue one");
    Expect(((topLeft >> 8) & 0xFF) == 255, "green lands in the green channel");
    Expect((topLeft & 0xFF) == 0, "blue lands in the blue channel");
    Expect(((topRight >> 16) & 0xFF) == 0, "the right half has no red, as it was written");
    Expect(((bottomLeft >> 8) & 0xFF) == 0, "the bottom half has no green, as it was written");
    Expect(((topLeft >> 24) & 0xFF) == 255, "an opaque png decodes fully opaque");

    ImageCodec::Release(image);
    Expect(image.pixels == nullptr, "releasing an image clears it");
}

static void CheckAlphaPng()
{
    std::vector<uint8_t> encoded;
    EncodePng(16, 16, true, encoded);

    ImageCodec::Image image;
    Expect(ImageCodec::Decode(encoded.data(), encoded.size(), image) == ImageCodec::Result::Ok, "a png with alpha decodes");
    Expect(image.hasAlpha, "a png with alpha is reported as having it");

    uint32_t halfLit = PixelAt(image, 12, 2);
    Expect(((halfLit >> 24) & 0xFF) == 128, "the alpha channel survives the round trip");
    Expect(((halfLit >> 8) & 0xFF) == 128, "a half transparent pixel comes back premultiplied, as a surface expects");

    uint32_t opaqueSide = PixelAt(image, 2, 2);
    Expect(((opaqueSide >> 16) & 0xFF) == 255, "a fully opaque pixel is left alone by premultiplying");

    ImageCodec::Release(image);
}

static void CheckWebp()
{
    const int size = 16;
    std::vector<uint8_t> raw(static_cast<size_t>(size) * size * 4);

    for (int y = 0; y < size; ++y)
    {
        for (int x = 0; x < size; ++x)
        {
            size_t offset = (static_cast<size_t>(y) * size + x) * 4;

            raw[offset] = 0;
            raw[offset + 1] = 0;
            raw[offset + 2] = x < size / 2 ? 255 : 0;
            raw[offset + 3] = 255;
        }
    }

    uint8_t* encoded = nullptr;
    size_t length = WebPEncodeLosslessBGRA(raw.data(), size, size, size * 4, &encoded);

    Expect(length > 0 && encoded != nullptr, "the test can write a webp to decode");
    if (length == 0) return;

    Expect(ImageCodec::Detect(encoded, length) == ImageCodec::Format::Webp, "the written webp is recognised");

    ImageCodec::Image image;
    Expect(ImageCodec::Decode(encoded, length, image) == ImageCodec::Result::Ok, "a webp decodes");
    Expect(image.width == size && image.height == size, "a webp keeps its size");
    Expect(((PixelAt(image, 2, 2) >> 16) & 0xFF) == 255, "a webp puts red in the red channel");
    Expect(((PixelAt(image, 12, 2) >> 16) & 0xFF) == 0, "a webp keeps the halves apart");

    ImageCodec::Release(image);
    WebPFree(encoded);
}

static void CheckJpeg()
{
    const int size = 16;
    std::vector<uint8_t> raw(static_cast<size_t>(size) * size * 3);

    for (int y = 0; y < size; ++y)
    {
        for (int x = 0; x < size; ++x)
        {
            size_t offset = (static_cast<size_t>(y) * size + x) * 3;

            raw[offset] = x < size / 2 ? 255 : 0;
            raw[offset + 1] = 0;
            raw[offset + 2] = 0;
        }
    }

    jpeg_compress_struct encoder;
    jpeg_error_mgr errors;

    encoder.err = jpeg_std_error(&errors);
    jpeg_create_compress(&encoder);

    unsigned char* encoded = nullptr;
    unsigned long length = 0;
    jpeg_mem_dest(&encoder, &encoded, &length);

    encoder.image_width = size;
    encoder.image_height = size;
    encoder.input_components = 3;
    encoder.in_color_space = JCS_RGB;

    jpeg_set_defaults(&encoder);
    jpeg_set_quality(&encoder, 95, TRUE);
    jpeg_start_compress(&encoder, TRUE);

    while (encoder.next_scanline < encoder.image_height)
    {
        JSAMPROW row = raw.data() + static_cast<size_t>(encoder.next_scanline) * size * 3;
        jpeg_write_scanlines(&encoder, &row, 1);
    }

    jpeg_finish_compress(&encoder);
    jpeg_destroy_compress(&encoder);

    Expect(length > 0 && encoded != nullptr, "the test can write a jpeg to decode");
    if (length == 0) return;

    Expect(ImageCodec::Detect(encoded, length) == ImageCodec::Format::Jpeg, "the written jpeg is recognised");

    ImageCodec::Image image;
    Expect(ImageCodec::Decode(encoded, length, image) == ImageCodec::Result::Ok, "a jpeg decodes");
    Expect(image.width == size && image.height == size, "a jpeg keeps its size");
    Expect(!image.hasAlpha, "a jpeg is reported opaque");
    Expect(((PixelAt(image, 2, 2) >> 16) & 0xFF) > 200, "a jpeg puts red in the red channel");
    Expect(((PixelAt(image, 13, 2) >> 16) & 0xFF) < 60, "a jpeg keeps the halves apart");
    Expect(((PixelAt(image, 2, 2) >> 24) & 0xFF) == 255, "a jpeg decodes fully opaque");

    ImageCodec::Release(image);
    free(encoded);
}

static void CheckMeasuring()
{
    std::vector<uint8_t> encoded;
    EncodePng(37, 11, false, encoded);

    uint32_t width = 0;
    uint32_t height = 0;

    Expect(ImageCodec::MeasureOnly(encoded.data(), encoded.size(), width, height) == ImageCodec::Result::Ok,
           "an image can be measured without being decoded");
    Expect(width == 37 && height == 11, "measuring reports the real size");
}

static void CheckRefusals()
{
    ImageCodec::Image image;

    const uint8_t rubbish[16] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    Expect(ImageCodec::Decode(rubbish, sizeof(rubbish), image) == ImageCodec::Result::UnknownFormat,
           "bytes of no known format are refused");

    std::vector<uint8_t> encoded;
    EncodePng(16, 16, false, encoded);

    for (size_t index = 40; index < encoded.size(); index += 3) encoded[index] ^= 0xFF;

    ImageCodec::Result outcome = ImageCodec::Decode(encoded.data(), encoded.size(), image);
    Expect(outcome == ImageCodec::Result::Corrupt || outcome == ImageCodec::Result::Ok,
           "a mangled png either decodes or is refused, but does not crash");

    if (outcome == ImageCodec::Result::Ok) ImageCodec::Release(image);

    std::vector<uint8_t> truncated;
    EncodePng(16, 16, false, truncated);
    truncated.resize(30);

    Expect(ImageCodec::Decode(truncated.data(), truncated.size(), image) == ImageCodec::Result::Corrupt,
           "a truncated png is refused rather than read past its end");
}

int main()
{
    printf("image codec:\n");

    CheckDetection();
    CheckOpaquePng();
    CheckAlphaPng();
    CheckWebp();
    CheckJpeg();
    CheckMeasuring();
    CheckRefusals();

    if (failures != 0)
    {
        printf("image-codec FAILED: %d of %d checks failed\n", failures, checks);
        return 1;
    }

    printf("image-codec PASSED: %d checks\n", checks);
    return 0;
}
