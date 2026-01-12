package org.hasting.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Utility class providing consistent styling and layout helpers for UI components.
 * 
 * <p>This class centralizes common UI styling patterns to ensure consistency
 * across all application views and components.
 * 
 * @since 2.0
 */
public class UIStyleHelper {
    
    /**
     * Creates a styled section container with title and description.
     * 
     * @param title The section title
     * @param description The section description
     * @return A VBox containing the styled section
     */
    public static VBox createStyledSection(String title, String description) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label titleLabel = createSectionTitle(title);
        section.getChildren().add(titleLabel);
        
        if (description != null && !description.trim().isEmpty()) {
            Label descLabel = createInstructionLabel(description);
            section.getChildren().add(descLabel);
        }
        
        return section;
    }
    
    /**
     * Creates a section title label with consistent styling.
     * 
     * @param text The title text
     * @return A styled Label for section titles
     */
    public static Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        label.setStyle("-fx-text-fill: #495057;");
        return label;
    }
    
    /**
     * Creates an instruction label with consistent styling.
     * 
     * @param text The instruction text
     * @return A styled Label for instructions
     */
    public static Label createInstructionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", 12));
        label.setStyle("-fx-text-fill: #6c757d;");
        label.setWrapText(true);
        return label;
    }
    
    /**
     * Creates a field label with consistent styling.
     * 
     * @param text The label text
     * @return A styled Label for form fields
     */
    public static Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.NORMAL, 12));
        label.setStyle("-fx-text-fill: #495057;");
        return label;
    }
    
    /**
     * Creates a horizontal container for buttons with consistent spacing.
     * 
     * @return An HBox configured for button layout
     */
    public static HBox createButtonContainer() {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);
        return container;
    }
    
    /**
     * Creates a vertical spacer with the specified height.
     * 
     * @param height The height of the spacer in pixels
     * @return A Region configured as a vertical spacer
     */
    public static Region createVerticalSpacer(double height) {
        Region spacer = new Region();
        spacer.setPrefHeight(height);
        return spacer;
    }
    
    /**
     * Creates a horizontal spacer that expands to fill available space.
     * 
     * @return A Region configured as a horizontal spacer
     */
    public static Region createHorizontalSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
    
    /**
     * Creates a styled separator for dividing sections.
     * 
     * @return A styled Separator
     */
    public static Separator createStyledSeparator() {
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #dee2e6;");
        return separator;
    }
}