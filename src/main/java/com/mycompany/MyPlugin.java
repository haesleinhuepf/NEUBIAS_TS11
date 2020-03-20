package com.mycompany;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

public class MyPlugin implements PlugInFilter {

    public int setup(String arg, ImagePlus imp) {
        if (arg.equals("about")) {
            showAbout(); return DONE;
        }
        return DOES_8G+DOES_STACKS+SUPPORTS_MASKING;
    } //setup

    private void showAbout() {
        System.out.println("Hello world.");
    }

    public void run(ImageProcessor ip) {
        ip.invert();

        /*
        byte[] pixels = (byte[])ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();

        int[][] inDataArrInt = ImageJUtility.convertFrom1DByteArr(pixels, width, height);
        int[] invertTF = ImageTransformationFilter.getInversionTF(255);
        int[][] invertedImg = ImageTransformationFilter.getTransformedImage(inDataArrInt, width, height, invertTF);

        ImageJUtility.showNewImage(invertedImg, width, height, "inverted image");
        */
    }


    public static void main(String[] args) {
        new ImageJ();
        IJ.open("https://imagej.nih.gov/ij/images/blobs.gif");

        ImagePlus currentImage = IJ.getImage();

        new MyPlugin().run(currentImage.getProcessor());
    }
}