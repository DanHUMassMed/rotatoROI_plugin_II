package com.amywalkerlab.rotato_roi_ii.process;

import java.awt.*
import java.awt.event.*

// Define a class that extends Dialog and implements ActionListener
class WaitForUserDialog extends Dialog implements ActionListener {

    private String buttonClicked = "None"  // Store which button was clicked
    private Button okButton
    private Button cancelButton

    // Constructor to set up the dialog
    WaitForUserDialog(String title, String message, Frame frame = new Frame()) {
        super(frame, title, false)  // Set false for non-modal
        
        // Keep the dialog on top
        setAlwaysOnTop(true)

        // Set up the dialog layout
        setLayout(new BorderLayout())
        setSize(400, 200)
        setLocationRelativeTo(null)  // Center the dialog

        // Create a non-editable TextArea for multiline message display
        TextArea messageArea = new TextArea(message, 5, 80, TextArea.SCROLLBARS_NONE)
        messageArea.setEditable(false)  // Make it non-editable to mimic label behavior
        messageArea.setBackground(Color.LIGHT_GRAY) 
        add(messageArea, BorderLayout.CENTER)

        // Create buttons and a panel to hold them
        Panel buttonPanel = new Panel()
        buttonPanel.setLayout(new FlowLayout())

        okButton = new Button("OK")
        cancelButton = new Button("Cancel")

        // Add buttons to the panel
        buttonPanel.add(okButton)
        buttonPanel.add(cancelButton)

        // Add the button panel to the dialog
        add(buttonPanel, BorderLayout.SOUTH)

        // Add action listeners for the buttons
        okButton.addActionListener(this)
        cancelButton.addActionListener(this)

        // Add a window listener to handle closing the dialog
        addWindowListener(new WindowAdapter() {
            @Override
            void windowClosing(WindowEvent we) {
                //dispose()  
                Toolkit.getDefaultToolkit().beep() 
            }
        })
    }

    // ActionListener method to handle button clicks
    @Override
    void actionPerformed(ActionEvent e) {
        if (e.source == okButton) {
            buttonClicked = "OK"
            println("You clicked OK")
        } else if (e.source == cancelButton) {
            buttonClicked = "Cancel"
            println("You clicked Cancel")
        }
        dispose()  // Close the dialog when either button is clicked
    }

    // Method to get which button was clicked
    String getButtonClicked() {
        return buttonClicked
    }

}

