package org.openpnp.machine.reference.vision.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.IOUtils;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator.PartFiducialPipeline;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.ImageInput;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceFiducialLocatorConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceFiducialLocator fiducialLocator;
    private final PartFiducialPipeline fiducialSettings;
    private final Part fiducialPart;

    private JButton editCustomPipelineButton;

    public ReferenceFiducialLocatorConfigurationWizard(ReferenceFiducialLocator fiducialLocator,
            Part part) {
        this.fiducialLocator = fiducialLocator;
        this.fiducialPart = part;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Fiducial Vision Settings", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("right:default"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblPipeline = new JLabel("Edit default (common) fiducial pipeline");
        JLabel lblReset = new JLabel("Reset default (common) fiducial pipeline");
        JButton editDefaultPipelineButton = new JButton("Edit");
        JButton resetDefaultPipelineButton = new JButton("Reset");
        
        // per part
        JLabel lblPart = new JLabel("Edit fiducial pipeline for this part");
        editCustomPipelineButton = new JButton("Edit");

        editDefaultPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editDefaultPipeline();
                });
            }
        });

        editCustomPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editCustomPipeline(part);
                });
            }
        });

        resetDefaultPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    resetDefaultPipeline();
                });
            }
        });

        panel.add(lblPipeline, "2, 2");
        panel.add(editDefaultPipelineButton, "4, 2");
        panel.add(lblReset, "2, 4");
        panel.add(resetDefaultPipelineButton, "4, 4");
        panel.add(lblPart, "2, 6");
        panel.add(editCustomPipelineButton, "4, 6");
        this.fiducialSettings = fiducialLocator.getFiducialSettings(part);
    }

    private void editDefaultPipeline() throws Exception {
        CvPipeline pipeline = fiducialLocator.getDefaultPipeline();
        editPipeline(pipeline);
    }

    private void resetDefaultPipeline() throws Exception {
        String xml = IOUtils.toString(ReferenceFiducialLocator.class
                .getResource("ReferenceFiducialLocator-DefaultPipeline.xml"));
        CvPipeline pipeline = new CvPipeline(xml);
        fiducialLocator.setDefaultPipeline(pipeline);
        editPipeline(pipeline);
    }

    private void editCustomPipeline(Part part) throws Exception {
        CvPipeline pipeline = fiducialLocator.getFiducialSettings(part).getPipeline();
        editPipeline(pipeline);
    }

    private void editPipeline(CvPipeline pipeline) {
        BufferedImage template = null;
        Camera camera = null;
        
        try {
            camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
        }
        catch (Exception ignored) {
        }
        if (pipeline.getCamera() == null) {
            pipeline.setCamera(camera);
        }

        try {
            Footprint fp = null;
            try {
                fp = fiducialPart.getPackage().getFootprint();
            }
            catch (Exception e) {
                fp = null;
            }
            if (fp != null) {
                CvStage templateStage = pipeline.getStage("template");
                if (templateStage != null) {
                    if (templateStage instanceof ImageInput) {
                        ImageInput imgIn = (ImageInput) templateStage;
                        template = fiducialSettings.getTemplate();
                        if (template == null)
                            template = ReferenceFiducialLocator.createTemplate(
                                    camera.getUnitsPerPixel(), fp, fiducialSettings.getTemplateRotation());
                        imgIn.setInputImage(template);
                    }
                }
            }
        }
        catch (Exception ignored) {
            Logger.info("Could not set \"template\" stage in fiducial pipeline to template image");
        }

        JDialog dialog = new JDialog(MainFrame.get(), "Fiducial Vision Pipeline");
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }

    @Override
    public void createBindings() {
    }

    @Override
    protected void loadFromModel() {
        super.loadFromModel();

        if (fiducialSettings.getPipeline() == null) {
            try {
                fiducialSettings.setPipeline(fiducialLocator.getDefaultPipeline().clone());
            }
            catch (Exception e) {
            }
        }
    }
}
