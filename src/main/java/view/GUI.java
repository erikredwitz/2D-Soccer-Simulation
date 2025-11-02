package view;

import model.Game;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;

public class GUI extends JFrame {
    public static final String PLAY_REPLAY = "Wiederholung abspielen";
    public static final String PAUSE_GAME = "Spiel pausieren";
    public static final String VIEW_LIVE = "Live-Ansicht";
    public static final String CONTINUE_GAME = "Spiel fortsetzen";
    private JPanel panel;
    private JLabel label;


    private JSlider replaySlider;

    private int oldSliderValue = 0;


    private Game game;
    private JPanel navigationPanel;
    private JSlider stepSizeSlider;
    private JButton stepBackButton;
    private JButton stepForwardButton;
    private JPanel stepPanel;
    private JButton playButton;
    private JCheckBox liveViewCheckBox;
    private JButton livePauseButton;

    private int playBackPosition;
    private volatile boolean playbackIsRunning = false;
    private volatile boolean isLiveRunning = false;

    public static final int PRELOAD_DISTANCE = 20;

    public GUI() {
        initializeComponents();
    }

    public void setImage(BufferedImage image) {
        EventQueue.invokeLater(() ->
            label.setIcon(new ImageIcon(image))
        );
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

        playButton = new JButton(PLAY_REPLAY);
        playButton.addActionListener(e -> playButtonPressed());
        stepPanel.add(playButton);

        liveViewCheckBox = new JCheckBox(VIEW_LIVE);
        liveViewCheckBox.addActionListener(e-> toggleLiveView());
        liveViewCheckBox.setSelected(true);
        stepPanel.add(liveViewCheckBox);

        livePauseButton = new JButton("Spiel starten");
        livePauseButton.addActionListener(e->toggleLivePause());
        stepPanel.add(livePauseButton);

        panel.setLayout(new GridLayout(2, 1));
        panel.add(navigationPanel);


        replaySlider = new JSlider(JSlider.HORIZONTAL,
            0, 0, 0);
        replaySlider.setPaintTicks(true);
        replaySlider.setPaintLabels(true);
        replaySlider.setMajorTickSpacing(50);
        replaySlider.setMinorTickSpacing(10);
        replaySlider.addChangeListener(e->sliderValueChanged());
        navigationPanel.add(replaySlider);

    }

    private void toggleLivePause() {
        isLiveRunning = !isLiveRunning;
        if(isLiveRunning){
            livePauseButton.setText(PAUSE_GAME);
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    game.startLive();
                    return null;
                }
            };
            worker.execute();
        }
        else{
            livePauseButton.setText(CONTINUE_GAME);
            game.pauseLive();
        }
    }

    private void toggleLiveView() {
        boolean isSelected = liveViewCheckBox.isSelected();
        game.setLiveViewActivated(isSelected);
        if(isSelected){
            stopPlayback();
            game.updateLiveView();
        } else{
            updateSliderLabels();
            game.setLiveViewActivated(false);
        }
    }


    private void sliderValueChanged() {
        if(!playbackIsRunning){
            EventQueue.invokeLater(() -> game.createAndSetFrame(replaySlider.getValue()));
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
                playBackPosition = replaySlider.getValue();
                while (playBackPosition < replaySlider.getMaximum() && playbackIsRunning) {
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
        replaySlider.setValue(Math.max(replaySlider.getValue() - stepSizeSlider.getValue(), replaySlider.getMinimum()));
    }

    private void stepForward() {
        replaySlider.setValue(Math.min(replaySlider.getValue() + stepSizeSlider.getValue(), replaySlider.getMaximum()));
    }


    private void updateSliderLabels() {
        EventQueue.invokeLater(()-> {
            int newMax = game.getNextTimeStep() - 1;
            replaySlider.setMaximum(newMax);
            int minorTickSpacing = 1 + newMax / 50;
            int majorTickSpacing = 4 * minorTickSpacing;
            replaySlider.setMinorTickSpacing(minorTickSpacing);
            replaySlider.setMajorTickSpacing(majorTickSpacing);
            replaySlider.setLabelTable(replaySlider.createStandardLabels(majorTickSpacing, 0));
            replaySlider.setPaintTicks(true);
            replaySlider.setPaintLabels(true);
            replaySlider.revalidate();
            replaySlider.repaint();
        });
    }


    public void setGame(Game game) {
        this.game = game;
    }


    private void playbackStep() {
        game.createAndSetFrame(playBackPosition + 1);
        playBackPosition+=stepSizeSlider.getValue();
        EventQueue.invokeLater(() -> replaySlider.setValue(playBackPosition));

    }
}