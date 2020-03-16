/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package de.mpicbg.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Plugin(type = Command.class, menuPath = "Plugins>Generic Dialog example")
public class CellCountingWorkflow<T extends RealType<T>> implements Command {

    private static int numericParameter = 2;

    @Override
    public void run() {
        if (!showDialog()) {
            return;
        }

        IJ.log("Performing something with " + numericParameter);

        GenericDialog anotherSecondDialog = new GenericDialog("another dialog");
        Panel panel = new Panel();
        Button button = new Button();
        button.setLabel("Button");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IJ.log("Hello world again");
                Checkbox checkbox = (Checkbox) anotherSecondDialog.getCheckboxes().get(0);
                checkbox.setState(!checkbox.getState());
            }
        });
        panel.add(button);

        ImagePlus imp = IJ.getImage();
        IJ.run("Gaussian Blur...");


        IJ.log("Bye");
    }

    private boolean showDialog() {
        GenericDialog gd = new GenericDialog("dialog");
        Panel panel = new Panel();
        Button button = new Button();
        button.setLabel("Button");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IJ.log("Hello world");
            }
        });
        panel.add(button);
        gd.addPanel(panel);
        gd.addNumericField("number", numericParameter, 2);
        gd.showDialog();
        if (gd.wasCanceled() ) {
            return false;
        }
        numericParameter = (int) gd.getNextNumber();
        return true;
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
        final File file = ij.ui().chooseFile(null, "open");

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
