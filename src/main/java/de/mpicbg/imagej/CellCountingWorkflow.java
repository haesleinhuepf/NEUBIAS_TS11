/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package de.mpicbg.imagej;

import bdv.util.BdvFunctions;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DoubleColumn;
import org.scijava.table.IntColumn;
import org.scijava.table.Table;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 */
@Plugin(type = Command.class, menuPath = "Plugins>Gauss Filtering")
public class CellCountingWorkflow<T extends RealType<T>> implements Command {

    @Parameter
    private Dataset currentData;

    @Parameter
    private ImageJ ij;

    @Override
    public void run() {
        final Img<T> image = (Img<T>)currentData.getImgPlus();

        System.out.println("Hello world.");

        // apply a gaussian blur
        RandomAccessibleInterval gaussFiltered = ij.op().filter().gauss(image, 2.0, 2.0);

        // show the blurred image
        ij.ui().show(gaussFiltered);

        // apply a threshold using Otsu's method
        IterableInterval otsuThresholded = ij.op().threshold().otsu(Views.iterable(gaussFiltered));

        ij.ui().show(otsuThresholded);

        // invert the image using a custom implementation of binaryNot
        invertBinaryImage(otsuThresholded);

        ij.ui().show(otsuThresholded);

        // apply connected components analysis
        RandomAccessibleInterval rai = ij.op().convert().bit(otsuThresholded);

        ImgLabeling ccaResult = ij.op().labeling().cca(rai, ConnectedComponents.StructuringElement.FOUR_CONNECTED);

        // convert the results to regions and count them
        LabelRegions<IntegerType> regions = new LabelRegions(ccaResult);

        int numberOfObjects = regions.getExistingLabels().size();

        System.out.print("Number of objects: " + numberOfObjects);

        // create columns for a results table
        IntColumn indexColum = new IntColumn();
        DoubleColumn meanIntensityColumn = new DoubleColumn();

        // create an image to show the labelling result
        long[] dimensions = new long[]{
                image.dimension(0),
                image.dimension(1)
        };
        Img<ARGBType> resultVisualisation = ArrayImgs.argbs(dimensions);

        Random random = new Random();



        // go through all regions
        int count = 0;
        for (LabelRegion region : regions) {
            long size = region.size();

            System.out.println("Object number " + count + " has size " + size);

            // get all pixels which belong to a region
            IterableInterval sampledRegion = Regions.sample(region, image);

            RealType measuredMeanIntensity = ij.op().stats().mean(sampledRegion);

            System.out.println("Intensity: " + measuredMeanIntensity.getRealFloat());

            // save measurement results to the columns of the results table
            indexColum.add(count);
            meanIntensityColumn.add((double) measuredMeanIntensity.getRealDouble());

            // paint all pixels of the current region in the result image with a given random colour
            IterableInterval pixelsInTheResultVisualisation = Regions.sample(region, resultVisualisation);

            Cursor cursor = pixelsInTheResultVisualisation.cursor();

            ARGBType randomColor = new ARGBType(random.nextInt());

            while(cursor.hasNext()) {
                ARGBType pixel = (ARGBType) cursor.next();
                pixel.set(randomColor);
            }

            count++;
        }

        // create the results table and show it
        Table table = new DefaultGenericTable();
        table.add(indexColum);
        table.add(meanIntensityColumn);

        table.setColumnHeader(0, "index");
        table.setColumnHeader(1, "Mean intensity");

        ij.ui().show(table);

        // save the table
        try {
            ij.io().save(table, "C:/structure/temp/output.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //ij.ui().show(resultVisualisation);
        //ImageJFunctions.show(resultVisualisation);

        // show the results image using BigDataViewer
        BdvFunctions.show(resultVisualisation, "Result");

    }

    private void invertBinaryImage(IterableInterval input) {
        Cursor cursor = input.cursor();

        while(cursor.hasNext()) {
            BitType pixel = (BitType) cursor.next();
            pixel.set(! pixel.get());
        }

    }

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // ask the user for a file to open
        final File file = new File("C:/structure/data/blobs.tif");
        //ij.ui().chooseFile(null, "open");

        if (file != null) {
            // load the dataset
            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());

            // show the image
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(CellCountingWorkflow.class, true);
        }
    }

}
