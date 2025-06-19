package view;

import model.Game;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import java.awt.Button;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

public class GUI extends JFrame {
    private JPanel panel;
    private JLabel label;
    private Button generateButton;

    private JSlider slider;

    private int oldSliderValue = 0;


    private Game game;
    private JPanel navigationPanel;
    private JSlider stepSizeSlider;
    private JButton stepBackButton;
    private JButton stepForwardButton;
    private JPanel stepPanel;
    private JButton playButton;
    private int lastImageTime = 0;
    private int playBackPosition;
    private boolean playbackIsRunning = false;

    public GUI() {
        initializeComponents();
    }

    public void setImage(BufferedImage image) {
        EventQueue.invokeLater(() ->
            label.setIcon(new ImageIcon(image))
        );
    }

    public void setImageForPlayback(BufferedImage image, int timeInHistory) {
        if (lastImageTime < timeInHistory) {
            setImage(image);
            lastImageTime = timeInHistory;
        }
    }

    private void initializeComponents() {
        panel = new JPanel();
        getContentPane().add(panel);
        label = new JLabel();
        panel.add(label);
        label.setVisible(true);
        navigationPanel = new JPanel();
        navigationPanel.setLayout(new GridLayout(3, 1));
        stepPanel = new JPanel();
        navigationPanel.add(stepPanel);
        stepSizeSlider = new JSlider(JSlider.HORIZONTAL,
            1, 20, 1);
        stepSizeSlider.setPaintTicks(true);
        stepSizeSlider.setPaintLabels(true);
        stepSizeSlider.addChangeListener(e -> stepSizeChanged());
        stepSizeSlider.setMajorTickSpacing(2);
        stepSizeSlider.setMinorTickSpacing(1);
        stepPanel.add(stepSizeSlider);

        stepBackButton = new JButton("- " + stepSizeSlider.getValue());
        stepBackButton.addActionListener(e -> stepBack());
        stepPanel.add(stepBackButton);

        stepForwardButton = new JButton("+ " + stepSizeSlider.getValue());
        stepForwardButton.addActionListener(e -> stepForward());
        stepPanel.add(stepForwardButton);

        playButton = new JButton("abspielen");
        playButton.addActionListener(e -> playButtonPressed());
        stepPanel.add(playButton);

        panel.setLayout(new GridLayout(2, 1));
        panel.add(navigationPanel);

        generateButton = new Button("weiter");
        generateButton.addActionListener(e -> generateNextSteps());
        slider = new JSlider(JSlider.HORIZONTAL,
            0, 0, 0);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);
        slider.addChangeListener(e->sliderValueChanged());
        navigationPanel.add(slider);
        navigationPanel.add(generateButton);

    }

    private void sliderValueChanged() {
        if(!playbackIsRunning){
            int time = slider.getValue();

        }
    }

    private void playButtonPressed() {
        if(playbackIsRunning){
            stopPlayback();
        }else{
            startPlayback();
        }
    }

    private void startPlayback() {
        playButton.setText("stoppen");
        playbackIsRunning = true;
        lastImageTime = 0;
        EventQueue.invokeLater(this::playUntilTheEnd);
    }

    private void stopPlayback() {
        playbackIsRunning = false;
        playButton.setText("abspielen");
    }

    private void playUntilTheEnd() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                playBackPosition = slider.getValue();
                while (playBackPosition < slider.getMaximum() && playbackIsRunning) {
                    playbackStep();
                    boolean hasWaited = false;
                    while (!hasWaited) {
                        try {
                            Thread.sleep(34);
                            hasWaited = true;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    //publish();
                }
                stopPlayback();
                return null;
            }
                /*
            protected void process() {
                setImage(game.getFrame());
            }

            @Override
            protected void done() {
                EventQueue.invokeLater(() -> slider.setValue(slider.getMaximum()));
            }*/
        };
        worker.execute();
    }

    private void stepSizeChanged() {
        stepForwardButton.setText("+ " + stepSizeSlider.getValue());
        stepBackButton.setText("- " + stepSizeSlider.getValue());
    }

    private void stepBack() {
        slider.setValue(Math.max(slider.getValue() - stepSizeSlider.getValue(), slider.getMinimum()));
    }

    private void stepForward() {
        slider.setValue(Math.min(slider.getValue() + stepSizeSlider.getValue(), slider.getMaximum()));
    }

    private void generateNextSteps() {
        if (game == null) {
            return;
        }
        for (int i = 0; i < 100; i++) {
            game.processNextTimeStep();
        }
        slider.setPaintLabels(false);
        slider.setPaintTicks(false);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setMaximum(game.getPointInTime() - 1);
        int minorTickSpacing = 1 + game.getPointInTime() / 50;
        int majorTickSpacing = 4 * (minorTickSpacing);
        slider.setMinorTickSpacing(minorTickSpacing);
        slider.setMajorTickSpacing(majorTickSpacing);
        slider.setLabelTable(slider.createStandardLabels(majorTickSpacing, 0));
    }


    public void setGame(Game game) {
        this.game = game;
    }


    private void playbackStep() {
        game.createAndSetFrameForPlayback(playBackPosition + 1);
        playBackPosition+=stepSizeSlider.getValue();
        EventQueue.invokeLater(() -> slider.setValue(playBackPosition));

    }
}