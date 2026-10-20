#include "ImageCodec/ImageCodec.hpp"

#include <csetjmp>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <vector>

#include <jpeglib.h>
#include <png.h>
#include <webp/decode.h>

static const uint8_t PNG_SIGNATURE[8] = {0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

ImageCodec::Format ImageCodec::Detect(const uint8_t* bytes, size_t length)
{
    if (!bytes) return Format::Unknown;

    if (length >= 8 && std::memcmp(bytes, PNG_SIGNATURE, 8) == 0) return Format::Png;
    if (length >= 3 && bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[2] == 0xFF) return Format::Jpeg;

    if (length >= 12 && std::memcmp(bytes, "RIFF", 4) == 0 && std::memcmp(bytes + 8, "WEBP", 4) == 0) return Format::Webp;

    return Format::Unknown;
}

const char* ImageCodec::FormatName(Format format)
{
    switch (format)
    {
        case Format::Png: return "png";
        case Format::Jpeg: return "jpeg";
        case Format::Webp: return "webp";
        default: return "unknown";
    }
}

const char* ImageCodec::ResultName(Result result)
{
    switch (result)
    {
        case Result::Ok: return "Ok";
        case Result::UnknownFormat: return "UnknownFormat";
        case Result::Corrupt: return "Corrupt";
        case Result::TooLarge: return "TooLarge";
        default: return "AllocationFailed";
    }
}

void ImageCodec::Release(Image& image)
{
    std::free(image.pixels);

    image.pixels = nullptr;
    image.width = 0;
    image.height = 0;
    image.stride = 0;
    image.hasAlpha = false;
}

static bool AllocateImage(ImageCodec::Image& image, uint32_t width, uint32_t height)
{
    if (width == 0 || height == 0) return false;
    if (width > ImageCodec::MAX_DIMENSION || height > ImageCodec::MAX_DIMENSION) return false;

    size_t stride = static_cast<size_t>(width) * 4;
    size_t total = stride * height;

    image.pixels = static_cast<uint8_t*>(std::calloc(total, 1));
    if (!image.pixels) return false;

    image.width = width;
    image.height = height;
    image.stride = static_cast<uint32_t>(stride);

    return true;
}

struct PngSource
{
    const uint8_t* bytes;
    size_t length;
    size_t offset;
};

static void ReadPngBytes(png_structp png, png_bytep into, png_size_t wanted)
{
    PngSource* source = static_cast<PngSource*>(png_get_io_ptr(png));

    if (!source || source->offset + wanted > source->length)
    {
        png_error(png, "read past the end of the image");
        return;
    }

    std::memcpy(into, source->bytes + source->offset, wanted);
    source->offset += wanted;
}

static void SwallowPngWarning(png_structp, png_const_charp)
{
}

static void EscapePngError(png_structp png, png_const_charp)
{
    longjmp(png_jmpbuf(png), 1);
}

static ImageCodec::Result DecodePng(const uint8_t* bytes, size_t length, ImageCodec::Image& image, bool measureOnly,
                                    uint32_t& measuredWidth, uint32_t& measuredHeight)
{
    png_structp png = png_create_read_struct(PNG_LIBPNG_VER_STRING, nullptr, &EscapePngError, &SwallowPngWarning);
    if (!png) return ImageCodec::Result::AllocationFailed;

    png_infop info = png_create_info_struct(png);
    if (!info)
    {
        png_destroy_read_struct(&png, nullptr, nullptr);
        return ImageCodec::Result::AllocationFailed;
    }

    std::vector<png_bytep> rows;

    if (setjmp(png_jmpbuf(png)))
    {
        png_destroy_read_struct(&png, &info, nullptr);
        if (!measureOnly) ImageCodec::Release(image);

        return ImageCodec::Result::Corrupt;
    }

    PngSource source{bytes, length, 0};
    png_set_read_fn(png, &source, &ReadPngBytes);
    png_read_info(png, info);

    png_uint_32 width = 0;
    png_uint_32 height = 0;
    int depth = 0;
    int colorType = 0;

    png_get_IHDR(png, info, &width, &height, &depth, &colorType, nullptr, nullptr, nullptr);

    if (measureOnly)
    {
        measuredWidth = width;
        measuredHeight = height;
        png_destroy_read_struct(&png, &info, nullptr);

        return ImageCodec::Result::Ok;
    }

    bool hasAlpha = colorType == PNG_COLOR_TYPE_RGB_ALPHA || colorType == PNG_COLOR_TYPE_GRAY_ALPHA
                    || png_get_valid(png, info, PNG_INFO_tRNS) != 0;

    if (depth == 16) png_set_strip_16(png);
    if (colorType == PNG_COLOR_TYPE_PALETTE) png_set_palette_to_rgb(png);
    if (colorType == PNG_COLOR_TYPE_GRAY && depth < 8) png_set_expand_gray_1_2_4_to_8(png);
    if (png_get_valid(png, info, PNG_INFO_tRNS)) png_set_tRNS_to_alpha(png);
    if (colorType == PNG_COLOR_TYPE_GRAY || colorType == PNG_COLOR_TYPE_GRAY_ALPHA) png_set_gray_to_rgb(png);

    png_set_filler(png, 0xFF, PNG_FILLER_AFTER);
    png_set_bgr(png);
    png_read_update_info(png, info);

    if (!AllocateImage(image, width, height))
    {
        png_destroy_read_struct(&png, &info, nullptr);
        return width > ImageCodec::MAX_DIMENSION || height > ImageCodec::MAX_DIMENSION ? ImageCodec::Result::TooLarge
                                                                                       : ImageCodec::Result::AllocationFailed;
    }

    rows.resize(height);
    for (png_uint_32 row = 0; row < height; ++row) rows[row] = image.pixels + static_cast<size_t>(row) * image.stride;

    png_read_image(png, rows.data());
    png_read_end(png, nullptr);
    png_destroy_read_struct(&png, &info, nullptr);

    image.hasAlpha = hasAlpha;
    return ImageCodec::Result::Ok;
}

struct JpegErrorSink
{
    jpeg_error_mgr manager;
    jmp_buf escape;
};

static void EscapeJpegError(j_common_ptr info)
{
    JpegErrorSink* sink = reinterpret_cast<JpegErrorSink*>(info->err);
    longjmp(sink->escape, 1);
}

static void SwallowJpegMessage(j_common_ptr)
{
}

static ImageCodec::Result DecodeJpeg(const uint8_t* bytes, size_t length, ImageCodec::Image& image, bool measureOnly,
                                     uint32_t& measuredWidth, uint32_t& measuredHeight)
{
    jpeg_decompress_struct decoder;
    JpegErrorSink sink;

    decoder.err = jpeg_std_error(&sink.manager);
    sink.manager.error_exit = &EscapeJpegError;
    sink.manager.output_message = &SwallowJpegMessage;

    if (setjmp(sink.escape))
    {
        jpeg_destroy_decompress(&decoder);
        if (!measureOnly) ImageCodec::Release(image);

        return ImageCodec::Result::Corrupt;
    }

    jpeg_create_decompress(&decoder);
    jpeg_mem_src(&decoder, bytes, static_cast<unsigned long>(length));

    if (jpeg_read_header(&decoder, TRUE) != JPEG_HEADER_OK)
    {
        jpeg_destroy_decompress(&decoder);
        return ImageCodec::Result::Corrupt;
    }

    if (measureOnly)
    {
        measuredWidth = decoder.image_width;
        measuredHeight = decoder.image_height;
        jpeg_destroy_decompress(&decoder);

        return ImageCodec::Result::Ok;
    }

    decoder.out_color_space = JCS_EXT_BGRA;
    jpeg_start_decompress(&decoder);

    if (!AllocateImage(image, decoder.output_width, decoder.output_height))
    {
        bool tooLarge = decoder.output_width > ImageCodec::MAX_DIMENSION || decoder.output_height > ImageCodec::MAX_DIMENSION;
        jpeg_destroy_decompress(&decoder);

        return tooLarge ? ImageCodec::Result::TooLarge : ImageCodec::Result::AllocationFailed;
    }

    while (decoder.output_scanline < decoder.output_height)
    {
        uint8_t* row = image.pixels + static_cast<size_t>(decoder.output_scanline) * image.stride;
        jpeg_read_scanlines(&decoder, &row, 1);
    }

    jpeg_finish_decompress(&decoder);
    jpeg_destroy_decompress(&decoder);

    image.hasAlpha = false;
    return ImageCodec::Result::Ok;
}

static ImageCodec::Result DecodeWebp(const uint8_t* bytes, size_t length, ImageCodec::Image& image, bool measureOnly,
                                     uint32_t& measuredWidth, uint32_t& measuredHeight)
{
    int width = 0;
    int height = 0;

    if (!WebPGetInfo(bytes, length, &width, &height)) return ImageCodec::Result::Corrupt;

    if (measureOnly)
    {
        measuredWidth = static_cast<uint32_t>(width);
        measuredHeight = static_cast<uint32_t>(height);

        return ImageCodec::Result::Ok;
    }

    if (!AllocateImage(image, static_cast<uint32_t>(width), static_cast<uint32_t>(height)))
    {
        bool tooLarge = static_cast<uint32_t>(width) > ImageCodec::MAX_DIMENSION
                        || static_cast<uint32_t>(height) > ImageCodec::MAX_DIMENSION;

        return tooLarge ? ImageCodec::Result::TooLarge : ImageCodec::Result::AllocationFailed;
    }

    uint8_t* decoded = WebPDecodeBGRAInto(bytes, length, image.pixels, static_cast<size_t>(image.stride) * image.height,
                                          static_cast<int>(image.stride));

    if (!decoded)
    {
        ImageCodec::Release(image);
        return ImageCodec::Result::Corrupt;
    }

    image.hasAlpha = true;
    return ImageCodec::Result::Ok;
}

static void PremultiplyImage(ImageCodec::Image& image)
{
    for (uint32_t y = 0; y < image.height; ++y)
    {
        uint32_t* row = reinterpret_cast<uint32_t*>(image.pixels + static_cast<size_t>(y) * image.stride);

        for (uint32_t x = 0; x < image.width; ++x)
        {
            uint32_t pixel = row[x];
            uint32_t alpha = (pixel >> 24) & 0xFFu;

            if (alpha == 255) continue;

            if (alpha == 0)
            {
                row[x] = 0;
                continue;
            }

            uint32_t blue = ((pixel & 0xFFu) * alpha + 127) / 255;
            uint32_t green = (((pixel >> 8) & 0xFFu) * alpha + 127) / 255;
            uint32_t red = (((pixel >> 16) & 0xFFu) * alpha + 127) / 255;

            row[x] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
    }
}

ImageCodec::Result ImageCodec::Decode(const uint8_t* bytes, size_t length, Image& image)
{
    uint32_t ignoredWidth = 0;
    uint32_t ignoredHeight = 0;
    Result outcome = Result::UnknownFormat;

    switch (Detect(bytes, length))
    {
        case Format::Png: outcome = DecodePng(bytes, length, image, false, ignoredWidth, ignoredHeight); break;
        case Format::Jpeg: outcome = DecodeJpeg(bytes, length, image, false, ignoredWidth, ignoredHeight); break;
        case Format::Webp: outcome = DecodeWebp(bytes, length, image, false, ignoredWidth, ignoredHeight); break;
        default: return Result::UnknownFormat;
    }

    if (outcome == Result::Ok && image.hasAlpha) PremultiplyImage(image);

    return outcome;
}

ImageCodec::Result ImageCodec::MeasureOnly(const uint8_t* bytes, size_t length, uint32_t& width, uint32_t& height)
{
    Image unused;

    switch (Detect(bytes, length))
    {
        case Format::Png: return DecodePng(bytes, length, unused, true, width, height);
        case Format::Jpeg: return DecodeJpeg(bytes, length, unused, true, width, height);
        case Format::Webp: return DecodeWebp(bytes, length, unused, true, width, height);
        default: return Result::UnknownFormat;
    }
}
