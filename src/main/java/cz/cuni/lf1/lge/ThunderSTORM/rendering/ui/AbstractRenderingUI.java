package cz.cuni.lf1.lge.ThunderSTORM.rendering.ui;

import cz.cuni.lf1.lge.ThunderSTORM.rendering.IncrementalRenderingMethod;
import cz.cuni.lf1.lge.ThunderSTORM.util.GridBagHelper;
import cz.cuni.lf1.lge.ThunderSTORM.util.MathProxy;
import cz.cuni.lf1.lge.ThunderSTORM.util.Range;
import cz.cuni.lf1.lge.ThunderSTORM.util.MacroUI.ParameterKey;
import cz.cuni.lf1.lge.ThunderSTORM.util.MacroUI.ParameterTracker;
import cz.cuni.lf1.lge.ThunderSTORM.util.MacroUI.validators.DoubleValidatorFactory;
import cz.cuni.lf1.lge.ThunderSTORM.util.MacroUI.validators.IntegerValidatorFactory;
import cz.cuni.lf1.lge.ThunderSTORM.util.MacroUI.validators.Validator;
import cz.cuni.lf1.lge.ThunderSTORM.util.MacroUI.validators.ValidatorException;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public abstract class AbstractRenderingUI extends IRendererUI {

    double sizeX, sizeY, left, top;
    JTextField zRangeTextField;
    JCheckBox threeDCheckBox;
    //parameters
    public ParameterKey.Double magnification;
    public ParameterKey.Integer repaintFrequency;
    public ParameterKey.Boolean threeD;
    public ParameterKey.Boolean colorizeZ;
    public ParameterKey.String zRange;
    public ParameterTracker.Condition threeDCondition = new ParameterTracker.Condition() {
        @Override
        public boolean isSatisfied() {
            return threeD.getValue();
        }

        @Override
        public ParameterKey[] dependsOn() {
            return new ParameterKey[]{threeD};
        }
    };
    protected boolean showRepaintFrequency = true;

    public AbstractRenderingUI() {
        magnification = parameters.createDoubleField("magnification", DoubleValidatorFactory.positiveNonZero(), 5);
        repaintFrequency = parameters.createIntField("repaint", IntegerValidatorFactory.positive(), 50, new ParameterTracker.Condition() {
            @Override
            public boolean isSatisfied() {
                return showRepaintFrequency;
            }

            @Override
            public ParameterKey[] dependsOn() {
                return null;
            }
        });
        threeD = parameters.createBooleanField("threeD", null, false);
        colorizeZ = parameters.createBooleanField("colorizeZ", null, false);
        zRange = parameters.createStringField("zrange", new Validator<String>() {
            @Override
            public void validate(String input) throws ValidatorException {
                try {
                    Range r = Range.parseFromStepTo(input);
                    int nSlices = (int) ((r.to - r.from) / r.step);
                    if(r.from > r.to) {
                        throw new RuntimeException("Z range \"from\" value (" + r.from + ") must be smaller than \"to\" value (" + r.to + ").");
                    }
                    if(nSlices < 1) {
                        throw new RuntimeException("Invalid range: Must have at least one slice.");
                    }
                } catch(RuntimeException ex) {
                    throw new ValidatorException(ex);
                }
            }
        }, "-500:100:500", threeDCondition);
    }

    public AbstractRenderingUI(double sizeX, double sizeY) {
        this();
        this.left = 0;
        this.top = 0;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }
    
    @Override
    public void setSize(double sizeX, double sizeY) {
        setSize(0, 0, sizeX, sizeY);
    }
    
    @Override
    public void setSize(double left, double top, double sizeX, double sizeY) {
        this.left = left;
        this.top = top;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    @Override
    public void setZRange(double from, double to) {
        if (!threeDCheckBox.isEnabled()) return;
        Range r = Range.parseFromStepTo(zRangeTextField.getText());
        r.from = roundDownTo(from, r.step);
        r.to = roundUpTo(to, r.step);
        zRangeTextField.setText(r.toStrFromStepTo());
    }

    protected double roundUpTo(double val, double mod) {
        return (val + mod - modulo(val, mod));
    }

    protected double roundDownTo(double val, double mod) {
        if (val > 0) {
            return (val - modulo(val, mod));
        } else {
            return (val - mod - modulo(val, mod));
        }
    }

    protected double modulo(double val, double mod) {
        double r = (int)(val / mod);
        return (val - r*mod);
    }

    @Override
    public int getRepaintFrequency() {
        return repaintFrequency.getValue();
    }

    public void setShowRepaintFrequency(boolean show) {
        this.showRepaintFrequency = show;
    }

    @Override
    public JPanel getOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        final JTextField resolutionTextField = new JTextField("", 20);
        parameters.registerComponent(magnification, resolutionTextField);
        final JTextField repaintFrequencyTextField = new JTextField("", 20);
        parameters.registerComponent(repaintFrequency, repaintFrequencyTextField);
        panel.add(new JLabel("Magnification:"), GridBagHelper.leftCol());
        panel.add(resolutionTextField, GridBagHelper.rightCol());

        if(showRepaintFrequency) {
            panel.add(new JLabel("Update frequency [frames]:"), GridBagHelper.leftCol());
            panel.add(repaintFrequencyTextField, GridBagHelper.rightCol());
        }

        final JLabel zRangeLabel = new JLabel("Z range (from:step:to) [nm]:");
        zRangeTextField = new JTextField("", 20);
        parameters.registerComponent(zRange, zRangeTextField);
        threeDCheckBox = new JCheckBox("", true);
        parameters.registerComponent(threeD, threeDCheckBox);
        final JLabel colorizeZLabel = new JLabel("Colorize z-stack:");
        final JCheckBox colorizeZCheckBox = new JCheckBox("", true);
        parameters.registerComponent(colorizeZ, colorizeZCheckBox);
        threeDCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                colorizeZLabel.setEnabled(threeDCheckBox.isSelected());
                colorizeZCheckBox.setEnabled(threeDCheckBox.isSelected());
                zRangeLabel.setEnabled(threeDCheckBox.isSelected());
                zRangeTextField.setEnabled(threeDCheckBox.isSelected());
            }
        });
        panel.add(new JLabel("3D:"), GridBagHelper.leftCol());
        panel.add(threeDCheckBox, GridBagHelper.rightCol());
        panel.add(colorizeZLabel, GridBagHelper.leftCol());
        panel.add(colorizeZCheckBox, GridBagHelper.rightCol());
        panel.add(zRangeLabel, GridBagHelper.leftCol());

        panel.add(zRangeTextField, GridBagHelper.rightCol());

        return panel;
    }

    @Override
    public IncrementalRenderingMethod getImplementation() {
        return getMethod();
    }

    protected abstract IncrementalRenderingMethod getMethod();
}
