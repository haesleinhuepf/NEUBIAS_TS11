/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package de.mpicbg.imagej;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.mesh.Mesh;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

/**
 *
 */
@Plugin(type = Command.class, menuPath = "Plugins>Gauss Filtering")
public class CellCountingWorkflow<T extends RealType<T>> implements Command {

    @Parameter
    private Dataset currentData;

    @Parameter
    ImageJ ij;

    @Override
    public void run() {
        final Img<T> image = (Img<T>)currentData.getImgPlus();

        // blur a bit to remove noise
        RandomAccessibleInterval gaussBlurred = ij.op().filter().gauss(image, 2, 2);

        // apply threshold
        IterableInterval otsuThresholed = ij.op().threshold().otsu(Views.iterable(gaussBlurred));


        // show intermediate result
        ij.ui().show(otsuThresholed);

        invertBinaryImage(otsuThresholed);
        //invertBinaryImage2D((RandomAccessibleInterval)otsuThresholed);

        // show result
        ij.ui().show(otsuThresholed);

        // connected components labelling for differentiating objects
        // aka Particle Analyser

        RandomAccessibleInterval otsuThresholedRai = ij.op().convert().int32(otsuThresholed);
        ImgLabeling cca = ij.op().labeling().cca(otsuThresholedRai, ConnectedComponents.StructuringElement.FOUR_CONNECTED);

        // get a list of regions
        LabelRegions<IntegerType> regions = new LabelRegions(cca);

        // determine number of cells
        int numberOfObjects = regions.getExistingLabels().size();
        System.out.print("Object found: " + numberOfObjects);

        // go through opbjects and measure intensity
        int count = 0;
        for (LabelRegion region : regions) {
            // get all pixels of the original image in the given region
            IterableInterval sample = Regions.sample(region, image);

            // measure mean intensity
            RealType mean = ij.op().stats().mean(sample);

            // output measurements
            System.out.println("Region " + count);
            System.out.println(" size " + region.size());
            System.out.println(" mean intensity" + mean.getRealFloat());







            count ++;
        }
    }

    private void invertBinaryImage(IterableInterval image) {

        // Inverting an image is a method which needs to be done for all pixels.
        // We can iterate through all pixels like this
        Cursor cursor = image.cursor();
        while (cursor.hasNext()) {
            BitType pixel = (BitType) cursor.next();
            pixel.set(!pixel.get());
        }

    }

    private void invertBinaryImage2D(RandomAccessibleInterval image) {

        RandomAccess ra = image.randomAccess();
        long[] position = new long[2];

        for (int x = 0; x < image.dimension(0); x++) {
            for (int y = 0; y < image.dimension(1); y++) {
                position[0] = x;
                position[1] = y;

                ra.setPosition(position);

                BitType pixel = (BitType) ra.get();
                pixel.set(!pixel.get());
            }
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
        final File file = new File("src/main/resources/blobs.tif");
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






































