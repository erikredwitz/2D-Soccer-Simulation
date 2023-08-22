package view;

import model.FrameCreator;

import java.awt.EventQueue;

public class Application {
    public static void main(String[] args) {
        var gui = new GUI();
        EventQueue.invokeLater(() -> gui.setVisible(true));
        FrameCreator frameCreator = new FrameCreator(gui);
    }
}
