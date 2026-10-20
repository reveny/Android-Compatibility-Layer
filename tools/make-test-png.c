#include <png.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
    if (argc < 2)
    {
        fprintf(stderr, "usage: make-test-png <output.png>\n");
        return 2;
    }

    const int width = 24;
    const int height = 24;

    FILE* file = fopen(argv[1], "wb");
    if (!file)
    {
        fprintf(stderr, "cannot write %s\n", argv[1]);
        return 1;
    }

    png_structp png = png_create_write_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    png_infop info = png_create_info_struct(png);

    if (setjmp(png_jmpbuf(png)))
    {
        png_destroy_write_struct(&png, &info);
        fclose(file);
        return 1;
    }

    png_init_io(png, file);
    png_set_IHDR(png, info, width, height, 8, PNG_COLOR_TYPE_RGB_ALPHA, PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT,
                 PNG_FILTER_TYPE_DEFAULT);
    png_write_info(png, info);

    png_bytep row = malloc((size_t) width * 4);

    for (int y = 0; y < height; y++)
    {
        for (int x = 0; x < width; x++)
        {
            int centreX = x - width / 2;
            int centreY = y - height / 2;
            int inside = centreX * centreX + centreY * centreY <= (width / 2 - 1) * (width / 2 - 1);

            row[x * 4] = inside ? 240 : 40;
            row[x * 4 + 1] = inside ? 90 : 40;
            row[x * 4 + 2] = inside ? 60 : 60;
            row[x * 4 + 3] = inside ? 255 : 0;
        }

        png_write_row(png, row);
    }

    png_write_end(png, NULL);
    png_destroy_write_struct(&png, &info);

    free(row);
    fclose(file);

    return 0;
}
