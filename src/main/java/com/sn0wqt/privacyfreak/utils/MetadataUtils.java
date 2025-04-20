package com.sn0wqt.privacyfreak.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.jpeg.iptc.JpegIptcRewriter;
import org.apache.commons.imaging.formats.jpeg.xmp.JpegXmpRewriter;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class MetadataUtils {
    /**
     * Reads metadata from various file formats (images, videos, audio)
     * using metadata-extractor library which automatically detects file type.
     */
    public static String readMetadata(InputStream in, String filename)
            throws IOException, ImageProcessingException {
        try {
            // buffer so we can read the entire stream
            byte[] data = in.readAllBytes();

            // ImageMetadataReader detects file type automatically
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(data));

            // Format the metadata as text
            StringBuilder sb = new StringBuilder();
            for (Directory dir : metadata.getDirectories()) {
                for (Tag tag : dir.getTags()) {
                    sb.append(dir.getName())
                            .append(" — ")
                            .append(tag.getTagName())
                            .append(" = ")
                            .append(tag.getDescription())
                            .append("\n");
                }
                if (dir.hasErrors()) {
                    for (String err : dir.getErrors()) {
                        sb.append("[ERROR in ")
                                .append(dir.getName())
                                .append("] ")
                                .append(err)
                                .append("\n");
                    }
                }
            }
            return sb.toString().trim();

        } catch (Exception e) {
            throw new IOException("Failed to read metadata from " + filename + ": " + e.getMessage(), e);
        }
    }

    /**
     * Strips metadata from JPEG images using Apache Commons Imaging ExifRewriter
     * without re-encoding the image data.
     * see
     * {@link https://dev.exiv2.org/projects/exiv2/wiki/The_Metadata_in_JPEG_files}
     * for more details on JPEG segments and metadata structures.
     * 
     * @param in original image bytes
     * @return InputStream of the metadata-stripped image
     */
    public static InputStream stripImageMetadata(InputStream in) throws IOException {

        // Set of markers to remove (e.g. JFIF(APP0), ICC (APP2))
        // 0xE0 = APP0 (JFIF), 0xE2 = APP2 (ICC)
        Set<Integer> markersToRemove = Set.of(0xE0, 0xE2);

        // 1) slurp all bytes so we can probe & re‑use
        byte[] data = in.readAllBytes();

        // 2) detect format
        String format;
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext())
                throw new IOException("Unrecognized image format");
            format = readers.next().getFormatName().toLowerCase();
        }

        if (!format.equals("jpeg") && !format.equals("jpg")) {
            throw new IOException("Only JPEG/JPG supported");
        }

        // Note: APP1 is EXIF, APP13 is IPTC, APPX is XMP

        try {
            // a) strip EXIF
            ByteArrayOutputStream exifOut = new ByteArrayOutputStream();
            new ExifRewriter()
                    .removeExifMetadata(data, exifOut);
            byte[] noExif = exifOut.size() > 0 ? exifOut.toByteArray() : data;

            // b) strip IPTC
            ByteArrayOutputStream iptcOut = new ByteArrayOutputStream();
            new JpegIptcRewriter()
                    .removeIptc(new ByteArrayInputStream(noExif), iptcOut);
            byte[] noIptc = iptcOut.size() > 0 ? iptcOut.toByteArray() : noExif;

            // c) strip XMP
            ByteArrayOutputStream xmpOut = new ByteArrayOutputStream();
            new JpegXmpRewriter()
                    .removeXmpXml(new ByteArrayInputStream(noIptc), xmpOut);
            byte[] noXmp = xmpOut.size() > 0 ? xmpOut.toByteArray() : noIptc;

            // d) prune unwanted segments (e.g. APP0, APP2 which are JFIF and ICC)
            byte[] clean = pruneJpegSegments(noXmp, markersToRemove);

            return new ByteArrayInputStream(clean);

        } catch (ImagingException e) {
            throw new IOException("Failed to strip JPEG metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Remove the APP0 (JFIF) and APP2 (ICC) segments from a JPEG byte array,
     * keeping all other segments and the compressed image data intact.
     * 
     * see {@link https://dev.exiv2.org/projects/exiv2/wiki/The_Metadata_in_JPEG_files}
     * for more details on JPEG segments and metadata structures.
     *
     * @param jpegBytes       the full JPEG file bytes
     * @param markersToRemove a Set of marker codes to drop (e.g. 0xE0, 0xE2)
     * @return the JPEG bytes with the specified segments removed
     */
    public static byte[] pruneJpegSegments(byte[] jpegBytes, Set<Integer> markersToRemove)
            throws IOException {
        // wrap for reading primitives
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(jpegBytes));
        // accumulate the output
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1) Copy the SOI (Start of Image) marker (two bytes: 0xFF, 0xD8)
        int first = in.readUnsignedByte();
        out.write(first);
        int second = in.readUnsignedByte();
        out.write(second);

        // 2) Loop through all segments until Start Of Scan (0xDA)
        while (true) {
            // read the marker prefix
            int prefix = in.readUnsignedByte();
            // if it's not 0xFF, we've gone off‑spec; break out
            if (prefix != 0xFF) {
                break;
            }

            // read the marker code
            int marker = in.readUnsignedByte();

            // if it's SOS (0xDA), copy it and then copy the rest of the bytes
            if (marker == 0xDA) {
                out.write(0xFF);
                out.write(marker);
                // read all remaining bytes (image data + EOI
                byte[] rest = in.readAllBytes();
                out.write(rest);
                break;
            }

            // otherwise read the length (two bytes, big‑endian)
            int lengthHigh = in.readUnsignedByte();
            int lengthLow = in.readUnsignedByte();
            int length = (lengthHigh << 8) + lengthLow;

            // read the payload (length includes these two bytes)
            byte[] payload = new byte[length - 2];
            in.readFully(payload);

            // if this marker is NOT in our remove set, write it back out
            if (!markersToRemove.contains(marker)) {
                out.write(0xFF);
                out.write(marker);
                out.write(lengthHigh);
                out.write(lengthLow);
                out.write(payload);
            }
        }

        in.close();
        return out.toByteArray();
    }

}