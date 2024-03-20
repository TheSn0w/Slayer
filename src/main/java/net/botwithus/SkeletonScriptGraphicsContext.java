package net.botwithus;

import net.botwithus.rs3.events.impl.VariableUpdateEvent;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.imgui.ImGui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SkeletonScriptGraphicsContext extends ScriptGraphicsContext {
    private SkeletonScript script;
    private boolean isScriptRunning = false;
    private long totalElapsedTime = 0;
    private Instant startTime;
    String component = String.valueOf(VarManager.getVarValue(VarDomainType.PLAYER, 183));
    private int startingSlayerPoints = -1; // Initialized to -1 to indicate it's unset
    private int currentSlayerPoints;
    private int differenceSlayerPoints;
    private List<String> targetItemNames = new ArrayList<>();
    private String selectedItem = "";


    private static float RGBToFloat(int rgbValue) {
        return rgbValue / 255.0f;
    }

    public SkeletonScriptGraphicsContext(ScriptConsole scriptConsole, SkeletonScript script) {
        super(scriptConsole);
        this.script = script;
        this.startTime = Instant.now();
        subscribeToGameVariableUpdates();

    }


    @Override
    public void drawSettings() {
        ImGui.PushStyleColor(0, RGBToFloat(173), RGBToFloat(216), RGBToFloat(230), 0.8f); // Button color
        ImGui.PushStyleColor(21, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Button color
        ImGui.PushStyleColor(18, RGBToFloat(173), RGBToFloat(216), RGBToFloat(230), 1.0f); // Checkbox Tick color
        ImGui.PushStyleColor(5, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Border Colour
        ImGui.PushStyleColor(2, RGBToFloat(0), RGBToFloat(0), RGBToFloat(0), 0.9f); // Background color
        ImGui.PushStyleColor(7, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Checkbox Background color
        ImGui.PushStyleColor(11, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Header Colour
        ImGui.PushStyleColor(22, RGBToFloat(64), RGBToFloat(67), RGBToFloat(67), 1.0f); // Highlighted button color
        ImGui.PushStyleColor(27, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //ImGUI separator Colour
        ImGui.PushStyleColor(30, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour
        ImGui.PushStyleColor(31, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour
        ImGui.PushStyleColor(32, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour
        ImGui.PushStyleColor(33, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour
        ImGui.PushStyleColor(34, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour


        ImGui.SetWindowSize(200.f, 200.f);
        if (ImGui.Begin("Snows Slayer", ImGuiWindowFlag.None.getValue())) {
            ImGui.PushStyleVar(1, 10.f, 5f);
            ImGui.PushStyleVar(2, 10.f, 5f); //spacing between side of window and checkbox
            ImGui.PushStyleVar(3, 10.f, 5f);
            ImGui.PushStyleVar(4, 10.f, 5f);
            ImGui.PushStyleVar(5, 10.f, 5f);
            ImGui.PushStyleVar(6, 10.f, 5f);
            ImGui.PushStyleVar(7, 10.f, 5f);
            ImGui.PushStyleVar(8, 10.f, 5f); //spacing between seperator and text
            ImGui.PushStyleVar(9, 10.f, 5f);
            ImGui.PushStyleVar(10, 10.f, 5f);
            ImGui.PushStyleVar(11, 10.f, 5f); // button sizes
            ImGui.PushStyleVar(12, 10.f, 5f);
            ImGui.PushStyleVar(13, 10.f, 5f);
            ImGui.PushStyleVar(14, 10.f, 5f); // spaces between options ontop such as overlays, debug etc
            ImGui.PushStyleVar(15, 10.f, 5f); // spacing between Text/tabs and checkboxes
            if (ImGui.BeginTabBar("Options", ImGuiWindowFlag.None.getValue())) {
                if (ImGui.BeginTabItem("Item Toggles", ImGuiWindowFlag.None.getValue())) {
                    if (isScriptRunning) {
                        if (ImGui.Button("Stop Script")) {
                            script.stopScript();
                            totalElapsedTime += Duration.between(startTime, Instant.now()).getSeconds();
                            isScriptRunning = false;
                        }
                    } else {
                        if (ImGui.Button("Start Script")) {
                            script.startScript();
                            startTime = Instant.now();
                            isScriptRunning = true;
                        }
                    }
                    long elapsedTime = isScriptRunning ? Duration.between(startTime, Instant.now()).getSeconds() + totalElapsedTime : totalElapsedTime;
                    ImGui.Text(String.format("Runtime: %02d:%02d:%02d", elapsedTime / 3600, (elapsedTime % 3600) / 60, elapsedTime % 60));

                    ImGui.SeparatorText("Statistics");
                    displayLoopCount();
                    ImGui.Separator();
                    String componentText11 = script.getComponent11Text();
                    ImGui.Text("Current Task: " + componentText11);
                    String componentText10 = script.getComponentText();
                    ImGui.Text("Kills Remaining: " + componentText10);
                    ImGui.Separator();
                    updateAndDisplaySlayerPoints(script);
                    ImGui.SeparatorText("Items to Pickup");

                    this.selectedItem = ImGui.InputText("Item name", this.selectedItem);

                    if (ImGui.Button("Add Item") && !this.selectedItem.isEmpty()) {
                        if (!this.script.getTargetItemNames().contains(this.selectedItem)) {
                            this.script.println("Adding \"" + this.selectedItem + "\" to target items.");
                            this.script.addItemName(this.selectedItem);
                            this.selectedItem = "";
                        }
                    }
                    ImGui.SameLine();
                    if (ImGui.Button("Add Keyword") && !this.selectedItem.isEmpty()) {
                        this.script.println("Adding \"" + this.selectedItem + "\" to target items.");
                        this.script.addItemName(this.selectedItem.toLowerCase()); // Add the keyword in lowercase for case-insensitive matching
                        this.selectedItem = ""; // Clear the input field after adding
                    }

                    if (!this.script.getTargetItemNames().isEmpty() && ImGui.ListBoxHeader("Pickup list", 0.0F, 100.0F)) {
                        for (String itemName : new ArrayList<>(this.script.getTargetItemNames())) {
                            if (ImGui.Button(itemName)) {
                                this.script.println("Removing \"" + itemName + "\" from target items.");
                                this.script.removeItemName(itemName);
                                break;
                            }
                        }
                        ImGui.ListBoxFooter();
                    }

                    ImGui.EndTabItem();
                }
                ImGui.EndTabBar();
            }
            ImGui.End();
        }
        ImGui.PopStyleVar(100);
        ImGui.PopStyleColor(100);
    }
    public class Pair<L, R> {
        private final L left;
        private final R right;

        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public L getLeft() { return left; }
        public R getRight() { return right; }

        // equals, hashCode, and toString methods as needed
    }


    private void displayLoopCount() {
        int loopCount = script.getLoopCounter();
        ImGui.Text("Number of Tasks Complete: " + loopCount);

        Duration elapsedTime = Duration.between(startTime, Instant.now());
        float runsPerHour = calculatePerHour(elapsedTime, loopCount);
        ImGui.Text(String.format("Tasks Per Hour: %.2f", runsPerHour));

    }

    private float calculatePerHour(Duration elapsed, int quantity) {
        long elapsedSeconds = elapsed.getSeconds();
        if (elapsedSeconds == 0) return 0;
        return (float) quantity / elapsedSeconds * 3600;
    }

    private void subscribeToGameVariableUpdates() {
        script.subscribe(VariableUpdateEvent.class, event -> {
            // Update Slayer Points if the event is for the SlayerPoints varbit
            if (event.getId() == 183 && VarManager.getVarDomain(event.getId()) == VarDomainType.PLAYER) {
                component = String.valueOf(VarManager.getVarValue(VarDomainType.PLAYER, 183));
            }
        });
    }
    public void updateAndDisplaySlayerPoints(SkeletonScript script) {
        if (startingSlayerPoints == -1) {
            startingSlayerPoints = script.getCurrentSlayerPoints(); // Initialize starting value
        }
        currentSlayerPoints = script.getCurrentSlayerPoints(); // Always fetch the latest value
        differenceSlayerPoints = currentSlayerPoints - startingSlayerPoints; // Calculate difference

        // Assuming you're inside a method that draws ImGui content
        ImGui.Text("Starting Slayer Points: " + startingSlayerPoints);
        ImGui.Text("Current Slayer Points: " + currentSlayerPoints);
        ImGui.SeparatorText("Slayer Points Earned this Session: " + differenceSlayerPoints);
    }
}
