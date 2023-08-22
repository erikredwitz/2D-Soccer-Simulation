package view;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;

public class GUI extends JFrame {
    private JPanel panel;
    private JLabel label;
    public GUI(){
        initializeComponents();
    }

    public void setImage(BufferedImage image){
        EventQueue.invokeLater(()->
        label.setIcon(new ImageIcon(image))
        );
    }
    private void initializeComponents() {
        panel = new JPanel();
        getContentPane().add(panel);
        label = new JLabel();
        panel.add(label);
        label.setVisible(true);
    }
}
