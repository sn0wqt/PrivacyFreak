package com.sn0wqt.privacyfreak;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.avi.AviMetadataReader;
import com.drew.imaging.mp4.Mp4MetadataReader;
import com.drew.imaging.quicktime.QuickTimeMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeInput;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;

public class Utils {

    /**
     * Reads metadata from images (JPEG, PNG, GIF, etc.) or
     * video containers (MP4, MOV, AVI, MKV) based on file extension.
     *
     * @param in       the file’s bytes
     * @param filename the original filename (used to pick the reader)
     * @return newline‐delimited "Directory — Tag = Value"
     */
    public static String readMetadata(InputStream in, String filename)
            throws IOException, ImageProcessingException {
        try {
            // buffer so we can re‑read for whichever reader we choose
            byte[] data = in.readAllBytes();
            Metadata metadata;

            String lower = filename.toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".m4a") || lower.endsWith(".m4v")) {
                metadata = Mp4MetadataReader.readMetadata(new ByteArrayInputStream(data));

            } else if (lower.endsWith(".mov")) {
                metadata = QuickTimeMetadataReader.readMetadata(new ByteArrayInputStream(data));

            } else if (lower.endsWith(".avi")) {
                metadata = AviMetadataReader.readMetadata(new ByteArrayInputStream(data));
            
            } else {
                // fallback to images or other formats
                metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(data));
            }

            // now dump it to text
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
            throw new IOException("Failed to read metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Strips all metadata from any common image format (JPEG, PNG, GIF, etc.)
     * and returns a new InputStream with the clean image data.
     */
    public static InputStream stripImageMetadata(InputStream in) throws IOException {
        // Read all bytes so we can probe and reload
        byte[] data = in.readAllBytes();

        // 1) Detect format
        String formatName;
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext())
                throw new IOException("Unrecognized image format");
            ImageReader reader = readers.next();
            formatName = reader.getFormatName();
            reader.dispose();
        }

        // 2) Load the image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null)
            throw new IOException("Could not decode image");

        // 3) Remove embedded ICC Profile if exists (by forcing to a standard RGB color
        // space)
        BufferedImage convertedImg = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null).filter(image, convertedImg);

        // 4) Write image cleanly
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
            if (!writers.hasNext())
                throw new IOException("No writer for format " + formatName);
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            writer.setOutput(ios);
            writer.write(null, new IIOImage(convertedImg, null, null), param);
            writer.dispose();
        }

        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Strips all metadata from a video by piping data through FFmpeg via Jaffree.
     *
     * @param in     original video bytes
     * @param format container format (e.g. "mp4", "matroska", "mov")
     * @return InputStream of the metadata‑stripped video
     */
    public static InputStream stripVideoMetadata(InputStream in, String format) throws IOException {
        var baos = new ByteArrayOutputStream();

        FFmpeg.atPath() // find ffmpeg on your PATH
                .addInput(PipeInput.pumpFrom(in))
                .addArgument("-map_metadata")
                .addArgument("-1")
                .addArgument("-c")
                .addArgument("copy")
                .addOutput(PipeOutput.pumpTo(baos)
                        .setFormat(format))
                .execute();

        return new ByteArrayInputStream(baos.toByteArray());
    }

}
