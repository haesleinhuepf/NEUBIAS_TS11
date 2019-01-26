/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package de.mpicbg.imagej;

import bdv.util.BdvFunctions;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.mesh.Mesh;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.FloatColumn;
import org.scijava.table.IntColumn;
import org.scijava.table.Table;

import java.io.File;
import java.util.Random;

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

        CLIJ clij = CLIJ.getInstance();

        // convert / send image to the GPU
        ClearCLBuffer input = clij.convert(image, ClearCLBuffer.class);

        // create memory on GPU to store intermediate results and output
        ClearCLBuffer gaussBlurredOnGPU = clij.create(input);
        ClearCLBuffer otsuThresholdedOnGPU = clij.create(input);

        // blur on GPU
        clij.op().blurFast(input, gaussBlurredOnGPU, 2f,2f, 0f);

        // threshold on GPU
        clij.op().automaticThreshold(gaussBlurredOnGPU, otsuThresholdedOnGPU, "Otsu");

        // unfortunately, we need to convert a byte image on GPU to a bit-image on CPU, this works by thresholding
        IterableInterval otsuResultFromGPU = Views.iterable(clij.convert(otsuThresholdedOnGPU, RandomAccessibleInterval.class));
        IterableInterval otsuThresholed = ij.op().threshold().apply(otsuResultFromGPU, new UnsignedByteType(0));


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

        // create a result image to show object segmentation
        Img<ARGBType> result = ArrayImgs.argbs(new long[]{image.dimension(0), image.dimension(1)});
        Random random = new Random();

        // measure the size of the labels and write them in a table
        IntColumn indexColumn = new IntColumn();
        FloatColumn areaColumn = new FloatColumn();
        FloatColumn averageColumn = new FloatColumn();

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
            indexColumn.add(count);
            areaColumn.add((float) region.size());
            averageColumn.add(mean.getRealFloat());

            ARGBType colour = new ARGBType(random.nextInt());

            IterableInterval<ARGBType> resultRegionIterable = Regions.sample(region, result);
            Cursor<ARGBType> cursor = resultRegionIterable.cursor();
            while (cursor.hasNext()) {
                cursor.next().set(colour);
            }

            count ++;
        }

        // show resulting object separation as image
        BdvFunctions.show(result, "Labelling");

        // show results as table
        Table table = new DefaultGenericTable();
        table.add(indexColumn);
        table.add(areaColumn);
        table.add(averageColumn);
        table.setColumnHeader(0, "Index");
        table.setColumnHeader(1, "Area in pixels");
        table.setColumnHeader(2, "Mean intensity");
        ij.ui().show(table);
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






































