package net.botwithus;

import net.botwithus.api.game.hud.Dialog;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.queries.builders.animations.SpotAnimationQuery;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.GroundItemQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.animation.SpotAnimation;
import net.botwithus.rs3.game.scene.entities.characters.PathingEntity;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.characters.player.Player;
import net.botwithus.rs3.game.scene.entities.item.GroundItem;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.util.RandomGenerator;
import net.botwithus.rs3.util.Regex;


import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.botwithus.rs3.game.Client.getLocalPlayer;

public class SkeletonScript extends LoopingScript {
    Pattern overloads = Pattern.compile(Regex.getPatternForContainsString("overload").pattern(), Pattern.CASE_INSENSITIVE);
    Pattern PrayerPotion = Pattern.compile("prayer|restore", Pattern.CASE_INSENSITIVE);
    private boolean scriptRunning = false;
    private Instant scriptStartTime;
    private BotState botState = BotState.IDLE;

    public int getLoopCounter() {
        return loopCounter;
    }

    private int loopCounter = 0;
    private List<String> targetItemNames = new ArrayList<>();

    public void addItemName(String itemName) {
        if (!targetItemNames.contains(itemName)) {
            targetItemNames.add(itemName);
        }
    }

    // Method to remove an item name
    public void removeItemName(String itemName) {
        targetItemNames.remove(itemName);
    }

    // Getter for the list (optional, depending on your needs)
    public List<String> getTargetItemNames() {
        return new ArrayList<>(targetItemNames); // Return a copy to prevent external modification
    }

    public int getCurrentSlayerPoints() {
        return VarManager.getVarbitValue(9071);
    }

    public String getComponentText() {
        try {
            String text = ComponentQuery.newQuery(1639).componentIndex(10).results().first().getText();
            return text != null ? text : "Not found";
        } catch (Exception e) {
            return "Error or not available";
        }
    }

    public String getComponent11Text() {
        try {
            String text = ComponentQuery.newQuery(1639).componentIndex(11).results().first().getText();
            return text != null ? text : "Not found";
        } catch (Exception e) {
            return "Error or not available";
        }
    }

    public SkeletonScript(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new SkeletonScriptGraphicsContext(getConsole(), this);
        this.loopDelay = RandomGenerator.nextInt(50, 198);
    }

    public void startScript() {
        println("Attempting to start script...");
        if (!scriptRunning) {
            scriptRunning = true;
            scriptStartTime = Instant.now();
            println("Script started at: " + scriptStartTime);
        } else {
            println("Attempted to start script, but it is already running.");
        }
    }

    public void stopScript() {
        if (scriptRunning) {
            scriptRunning = false;
            Instant stopTime = Instant.now();
            println("Script stopped at: " + stopTime);
            long duration = Duration.between(scriptStartTime, stopTime).toMillis();
            println("Script ran for: " + duration + " milliseconds.");
        } else {
            println("Attempted to stop script, but it is not running.");
        }
    }

    enum BotState {
        IDLE,
        WARS_RETREAT,
        LANIAKEA,
        HANDLER,
    }


    @Override
    public void onLoop() {
        if (getLocalPlayer() != null && Client.getGameState() == Client.GameState.LOGGED_IN) {

            if (!scriptRunning) {
                return;
            }
            DeathsOffice();
            loot();
            println("Slayer Level: " + Skills.SLAYER.getSkill().getLevel());
            logBoss(getLocalPlayer());
        }
        switch (botState) {
            case IDLE -> {
                checkTaskCompletion();
            }
            case WARS_RETREAT -> {
                IdleDelays();
            }
            case LANIAKEA -> {
                ++loopCounter;
                TeleporttoLaniakea();
            }
            case HANDLER -> {
                handleTask();
            }
        }
    }

    private void loot() {
        if (getLocalPlayer() == null) {
            return;
        }

        SpotAnimationQuery.newQuery().ids(4419).results().stream()
                .findFirst()
                .ifPresent(this::attemptToPickUpItemOnSpotAnimation);

        Execution.delay(RandomGenerator.nextInt(601, 998));

        collectAllTargetItems(targetItemNames);
    }

    private void collectAllTargetItems(List<String> targetKeywords) {
        List<String> availableItems = getAvailableItems();
        List<String> itemsToCollect = new ArrayList<>();

        for (String item : availableItems) {
            for (String keyword : targetKeywords) {
                if (item.toLowerCase().contains(keyword)) {
                    itemsToCollect.add(item);
                    break;
                }
            }
        }

        collect(itemsToCollect);
    }

    private List<String> getAvailableItems() {
        List<String> items = new ArrayList<>();
        ResultSet<GroundItem> groundItems = GroundItemQuery.newQuery().results();

        for (net.botwithus.rs3.game.scene.entities.item.GroundItem item : groundItems) {
            items.add(item.getName());
        }
        return items;
    }

    private void collect(List<String> itemNames) {
        if (Backpack.isFull()) {
            manageFullBackpack();
        }
        ResultSet<GroundItem> groundItems = GroundItemQuery.newQuery().results();

        for (net.botwithus.rs3.game.scene.entities.item.GroundItem groundItem : groundItems) {
            if (itemNames.contains(groundItem.getName())) {

                groundItem.interact("Take");
                Execution.delayUntil(15000, () -> GroundItemQuery.newQuery().name(groundItem.getName()).results().isEmpty());
            }
        }
    }


    private void attemptToPickUpGroundItem(GroundItem groundItem) {
        if (Backpack.isFull()) {
            manageFullBackpack();
        }

        if (!Backpack.isFull() && groundItem != null) {
            println("Attempting to take ground item: " + groundItem.getName());
            boolean interactionSuccess = groundItem.interact("Take");
            Execution.delay(RandomGenerator.nextInt(1000, 2000));
            if (interactionSuccess) {
                Execution.delayUntil(15000, () -> GroundItemQuery.newQuery().name(groundItem.getName()).results().isEmpty());
            }
        }
    }

    private void manageFullBackpack() {
        println("Backpack is full, attempting to eat food.");
        ResultSet<Item> foodItems = InventoryItemQuery.newQuery().option("Eat").results();
        if (!foodItems.isEmpty()) {
            Item food = foodItems.first();
            if (food != null) {
                eatFood(food);
            }
        } else {
            println("No food to eat, retreating.");
            botState = BotState.WARS_RETREAT;
        }
    }

    private void eatFood(Item food) {
        println("Attempting to eat " + food.getName());
        boolean success = Backpack.interact(food.getName(), 1);
        if (success) {
            println("Eating " + food.getName());
            Execution.delayUntil(RandomGenerator.nextInt(2000, 2500), () -> !Backpack.isFull());
        } else {
            println("Failed to eat " + food.getName());
            Execution.delayUntil(RandomGenerator.nextInt(2000, 2500), () -> !Backpack.isFull());
        }
    }

    private void attemptToPickUpItemOnSpotAnimation(SpotAnimation spotAnimation) {
        GroundItem groundItem = GroundItemQuery.newQuery()
                .onTile(spotAnimation.getCoordinate())
                .results()
                .nearest();
        if (groundItem != null) {
            attemptToPickUpGroundItem(groundItem);
        }
    }


    private void checkTaskCompletion() {
        String taskCompletionText = ComponentQuery.newQuery(1639).componentIndex(10).results().first().getText();
        String component = ComponentQuery.newQuery(1639).componentIndex(11).results().first().getText();

        if (!taskCompletionText.equals("0")) {
            println("Current task: " + component + "  has: " + taskCompletionText + " kills left.");
            botState = BotState.HANDLER;
        } else {
            botState = BotState.WARS_RETREAT;
            println("Task completed.");
        }
    }

    private void logBoss(LocalPlayer localPlayer) {
        if (localPlayer == null) return;

        if (localPlayer.getTarget() != null) {
            int animationId = localPlayer.getTarget().getAnimationId();
            String targetName = localPlayer.getTarget().getName();
            println("Animation: " + animationId + ", Target Name: " + targetName);
        } else {
            println("No target currently.");
        }
    }

    Pattern slayerCape = Pattern.compile("slayer cape", Pattern.CASE_INSENSITIVE);

    private void TeleporttoLaniakea() {
        if (getLocalPlayer() == null) return;
        if (!InventoryItemQuery.newQuery().ids(93).name(slayerCape).results().isEmpty()) {
            navigateTo(new Coordinate(5667, 2138, 0));
        } else {
            navigateTo(new Coordinate(5458, 2354, 0));
        }
        getTask();
    }


    private void getTask() {
        Npc laniakea = NpcQuery.newQuery().name("Laniakea").results().nearest();

        if (laniakea != null) {
            laniakea.interact("Get task");

            boolean taskInterfaceOpened = Execution.delayUntil(10000, () -> Interfaces.isOpen(1191));

            if (taskInterfaceOpened) {
                MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 78053391);
                Execution.delay(RandomGenerator.nextInt(2000, 3000));

                MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77856776);
                Execution.delay(RandomGenerator.nextInt(1500, 2500));
                botState = BotState.HANDLER;
            } else {
                botState = BotState.LANIAKEA;
                println("Failed to open the task interface.");
            }
        } else {
            botState = BotState.LANIAKEA;
            println("Laniakea is not found.");
        }
    }

    private void IdleDelays() {
        if (getLocalPlayer() != null)
            if (getLocalPlayer().getCoordinate().getRegionId() != 13214) {
                useWarsRetreat();
            } else {
                handleCampfire();
            }
        Execution.delay(RandomGenerator.nextInt(1000, 2000));
        println("We're idle!");
        Execution.delay(RandomGenerator.nextInt(1000, 2000));
    }

    private void useWarsRetreat() {
        if (getLocalPlayer() != null) {
            ScriptConsole.println("Used Wars Retreat: " + ActionBar.useAbility("War's Retreat Teleport"), new Object[0]);
            Execution.delay(RandomGenerator.nextInt(4000, 5000));
            DeHandleSoulSplit();
            DeActivateMagicPrayer();
            DeActivateMeleePrayer();
            DeActivateRangedPrayer();
            handleCampfire();
        }
    }

    private void handleCampfire() {
        EntityResultSet<SceneObject> Campfire = SceneObjectQuery.newQuery().name("Campfire").option("Warm hands").results();
        if (getLocalPlayer() == null)
            return;
        ComponentQuery query = ComponentQuery.newQuery(284).spriteId(10931);
        ResultSet<Component> results = query.results();

        if (results.isEmpty() && !Campfire.isEmpty()) {
            SceneObject campfire = Campfire.nearest();
            if (campfire != null) {
                campfire.interact("Warm hands");
                println("Warming hands!");
                Execution.delayUntil(10000, () -> !results.isEmpty());
                handlePraying();
            }
        } else if (!results.isEmpty()) {
            println("Campfire buff is already active!");
            handlePraying();
        }
    }

    private void handlePraying() {

        EntityResultSet<SceneObject> altarOfWarResults = SceneObjectQuery.newQuery().name("Altar of War").results();

        if (!altarOfWarResults.isEmpty()) {
            SceneObject altar = altarOfWarResults.nearest();
            if (altar != null && altar.interact("Pray")) {
                println("Praying at Altar of War!");
                Execution.delay(RandomGenerator.nextInt(4000, 5000));
                handleBank();
            }
        }
    }

    private void handleBank() {
        EntityResultSet<SceneObject> BankChest = SceneObjectQuery.newQuery().name("Bank chest").results();
        if (getLocalPlayer() == null)
            return;
        if (!BankChest.isEmpty()) {
            SceneObject bank = BankChest.nearest();
            if (bank != null) {
                bank.interact("Load Last Preset from");
                println("Loading preset!");
                Execution.delay(RandomGenerator.nextInt(3000, 5000));

                boolean healthFull = Execution.delayUntil(15000, () -> getLocalPlayer().getCurrentHealth() == getLocalPlayer().getMaximumHealth());
                if (healthFull) {
                    println("Player health is now full.");
                } else {
                    println("Timed out waiting for player health to be full.");
                }
            }
            if (ComponentQuery.newQuery(1639).componentIndex(10).results().first().getText().equals("0")) {
                botState = BotState.LANIAKEA;
            } else {
                botState = BotState.HANDLER;
            }
        }
    }

    private void handleTask() {
        Component component = ComponentQuery.newQuery(1639).componentIndex(11).results().first();
        if (component != null) {
            String taskText = component.getText().trim().toLowerCase();

            switch (taskText) {
                case "creatures of the lost grove":
                    Vinecrawlers();
                    break;
                case "aviansies":
                    Avianses();
                    break;
                case "risen ghosts":
                    RisenGhosts();
                    println("Moving to Risen Ghosts");
                    break;
                case "undead":
                    RisenGhosts();
                    break;
                case "ganodermic creatures":
                    GanodermicCreatures();
                    break;
                case "dark beasts":
                    DarkBeasts();
                    break;
                case "glacors":
                    Glacor();
                    break;
                case "crystal shapeshifters":
                    CrystalShapeshifer();
                    break;
                case "nodon dragonkin":
                    NodonDragonkin();
                    break;
                case "soul devourers":
                    SoulDevourers();
                    break;
                case "dinosaurs":
                    Dinosaur();
                    break;
                case "mithril dragons":
                    MithrilDragons();
                    break;
                case "demons":
                    Demons();
                    break;
                case "ascension members":
                    OrderOfAscension();
                    break;
                case "kalphite":
                    Kalphites();
                    break;
                case "elves":
                    Elves();
                    break;
                case "shadow creatures":
                    ShadowCreatures();
                    break;
                case "vile blooms":
                    VileBlooms();
                    break;
                case "ice strykewyrms":
                    IceStrykewyrms();
                    break;
                case "lava strykewyrms":
                    LavaStrykewyrms();
                    break;
                case "greater demons":
                    GreaterDemons();
                    break;
                case "mutated jadinkos":
                    MutatedJadinkos();
                    break;
                case "corrupted creatures":
                    CorruptedCreatures();
                    break;
                case "iron dragons":
                    IronDragons();
                    break;
                case "adamant dragons":
                    AdamantDragons();
                    break;
                case "black demons":
                    BlackDemons();
                    break;
                case "kal'gerion demons":
                    KalgerionDemons();
                    break;
                case "gargoyles":
                    Gargoyle();
                    break;
                case "ripper demons":
                    RipperDemons();
                    break;
                default:
                    println("Task not recognized.");
                    return;
            }
            botState = BotState.IDLE;
        } else {
            println("The specified component was not found.");
        }
    }


    private void Vinecrawlers() {
        Coordinate topLeft = new Coordinate(1300, 5626, 0);
        Coordinate bottomRight = new Coordinate(1329, 5604, 0);
        Coordinate InteractionCoords = new Coordinate(2222, 3055, 0);

        if (Client.getLocalPlayer() != null) {
            Coordinate playerCoordinate = Client.getLocalPlayer().getCoordinate();

            boolean isWithinArea = playerCoordinate.getX() >= topLeft.getX() &&
                    playerCoordinate.getX() <= bottomRight.getX() &&
                    playerCoordinate.getY() <= topLeft.getY() &&
                    playerCoordinate.getY() >= bottomRight.getY();

            if (!isWithinArea) {
                Coordinate destination = new Coordinate(
                        (topLeft.getX() + bottomRight.getX()) / 2,
                        (topLeft.getY() + bottomRight.getY()) / 2,
                        0
                );
                Movement.traverse(NavPath.resolve(InteractionCoords));
                Execution.delayUntil(360000, () -> InteractionCoords.equals(Client.getLocalPlayer().getCoordinate()));
                if (Client.getLocalPlayer().getCoordinate().equals(InteractionCoords)) {
                    SceneObject Standstone = SceneObjectQuery.newQuery().name("Standstone").results().nearest();
                    Standstone.interact("Inspect");
                    println("Inspecting Standstone");
                    Execution.delay(RandomGenerator.nextInt(5000, 7500));
                    navigateTo(destination);
                }
            }
            if (isWithinArea) {
                ActivateMagicPrayer();
                KeepHydrated();
                Npc vinecrawler = NpcQuery.newQuery().name("Vinecrawler").results().nearest();

                if (!getLocalPlayer().hasTarget() && vinecrawler != null && !Client.getLocalPlayer().hasTarget()) {
                    vinecrawler.interact("Attack");
                    println("Attacking Vinecrawler");
                    Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                        PathingEntity<?> target = getLocalPlayer().getTarget();
                        return target == null || target.getCurrentHealth() == 0;
                    });
                }
            }

            Execution.delay(RandomGenerator.nextInt(601, 998));
        }
    }

    private void Avianses() {
        Coordinate topLeft = new Coordinate(2836, 5288, 0);
        Coordinate bottomRight = new Coordinate(2858, 5264, 0);

        if (Client.getLocalPlayer() != null) {
            Coordinate playerCoordinate = Client.getLocalPlayer().getCoordinate();

            boolean isWithinArea = playerCoordinate.getX() >= topLeft.getX() &&
                    playerCoordinate.getX() <= bottomRight.getX() &&
                    playerCoordinate.getY() <= topLeft.getY() &&
                    playerCoordinate.getY() >= bottomRight.getY();

            if (!isWithinArea) {
                Coordinate destination = new Coordinate(
                        (topLeft.getX() + bottomRight.getX()) / 2,
                        (topLeft.getY() + bottomRight.getY()) / 2,
                        0
                );
                Movement.traverse(NavPath.resolve(destination));
                println("Moving towards target area");
                Execution.delayUntil(360000, () -> destination.equals(Client.getLocalPlayer().getCoordinate()));
            }

            if (isWithinArea) {
                Npc aviansie = NpcQuery.newQuery().name("Aviansie").results().random();

                if (!getLocalPlayer().hasTarget() && aviansie != null && !Client.getLocalPlayer().hasTarget()) {
                    aviansie.interact("Attack");
                    println("Attacking Aviansie");
                    Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                        PathingEntity<?> target = getLocalPlayer().getTarget();
                        return target == null || target.getCurrentHealth() == 0;
                    });
                }
            }
            Execution.delay(RandomGenerator.nextInt(601, 998));
        }
    }

    private void RisenGhosts() {
        if (getLocalPlayer() == null) {
            return;
        }
        ActivateMagicPrayer();
        KeepHydrated();

        Coordinate ghostCoords = new Coordinate(3287, 3575, 0);
        Coordinate destination = new Coordinate(3289, 3610, 0);

        EntityResultSet<Npc> ghostResults = NpcQuery.newQuery().name("Risen ghost").results();
        if (ghostResults.isEmpty()) {
            println("Navigating to Risen Ghosts");
            navigateTo(ghostCoords);
            SceneObjectQuery.newQuery().name("Gates").results().nearest().interact("Open");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(destination);
            SceneObjectQuery.newQuery().name("Wilderness Crypt Entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
        } else {
            Npc ghost = ghostResults.nearest();
            if (!getLocalPlayer().hasTarget() && ghost != null && ghost.interact("Attack")) {
                println("Attacking Risen Ghost");
            }

            Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                PathingEntity<?> target = getLocalPlayer().getTarget();
                return target == null || target.getCurrentHealth() == 0;
            });
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void MovetoGanodermicCreatures() {
        if (Client.getLocalPlayer() == null) {
            return;
        }
        Coordinate GanodermicBeast = new Coordinate(4634, 5448, 0);

        navigateTo(GanodermicBeast);
        GanodermicCreatures();
    }

    private void GanodermicCreatures() {
        Npc ganodermicCreature = NpcQuery.newQuery().name("Ganodermic beast").results().nearest();
        if (Client.getLocalPlayer() == null) {
            return;
        }
        if (ganodermicCreature == null) {
            MovetoGanodermicCreatures();
        }
        if (!getLocalPlayer().hasTarget() && ganodermicCreature != null && !Client.getLocalPlayer().hasTarget()) {
            ganodermicCreature.interact("Attack");
            println("Attacking Ganodermic beast");
            Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                PathingEntity<?> target = getLocalPlayer().getTarget();
                return target == null || target.getCurrentHealth() == 0;
            });
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }


    private void DarkBeasts() {
        if (getLocalPlayer() == null) {
            return;
        }
        Coordinate DungeonEntrance = new Coordinate(1686, 5287, 1);
        Coordinate Gap = new Coordinate(1641, 5260, 0);
        Coordinate Barrier = new Coordinate(1651, 5279, 0);


        EntityResultSet<Npc> DarkBeasts = NpcQuery.newQuery().name("Dark beast").results();
        if (DarkBeasts.isEmpty()) {
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Cave").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Gap);
            SceneObjectQuery.newQuery().name("Gap").results().nearest().interact("Run-across");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Barrier);
            SceneObjectQuery.newQuery().name("Energy barrier").results().nearest().interact("Pass");
        } else {
            KeepHydrated();
            HandleSoulSplit();
            Npc Beasts = DarkBeasts.nearest();
            if (!getLocalPlayer().hasTarget() && Beasts != null && Beasts.interact("Attack")) {
                println("Attacking Dark Beast");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void navigateTo(Coordinate destination) {
        if (getLocalPlayer() == null) {
            return;
        }
        println("Navigating to " + destination);
        Movement.traverse(NavPath.resolve(destination));
        Execution.delayUntil(360000, () -> destination.equals(Client.getLocalPlayer().getCoordinate()));
    }

    private void Glacor() {
        if (getLocalPlayer() == null) {
            return;
        }
        HandleSoulSplit();
        Coordinate glacorCoords = new Coordinate(2997, 3844, 0);

        String[] glacyteNames = {"Enduring glacyte", "Sapping glacyte", "Unstable glacyte"};
        for (String glacyteName : glacyteNames) {
            EntityResultSet<Npc> glacytes = NpcQuery.newQuery().name(glacyteName).results();
            if (!glacytes.isEmpty()) {
                Npc glacyte = glacytes.nearest();
                if (!getLocalPlayer().hasTarget() && glacyte != null && glacyte.interact("Attack")) {
                    println("Attacking " + glacyteName);
                    Execution.delayUntil(RandomGenerator.nextInt(5000, 7500), glacyte::validate);
                    return;
                }
            }
        }

        EntityResultSet<Npc> glacorResults = NpcQuery.newQuery().name("Glacor").results();
        if (glacorResults.isEmpty()) {
            navigateTo(glacorCoords);
            println("Navigating to Glacor");
        } else {

            Npc glacor = glacorResults.nearest();
            if (!getLocalPlayer().hasTarget() && glacor != null && glacor.interact("Attack")) {
                println("Attacking Glacor");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void CrystalShapeshifer() {
        if (getLocalPlayer() == null) {
            return;
        }
        HandleSoulSplit();
        KeepHydrated();
        Coordinate WorldGate = new Coordinate(2367, 3358, 0);
        Coordinate crystalShapeshifterCoords = new Coordinate(4143, 6562, 0);

        EntityResultSet<Npc> shapeshifterResults = NpcQuery.newQuery().name("Crystal Shapeshifter").results();
        if (shapeshifterResults.isEmpty()) {
            navigateTo(WorldGate);
            SceneObjectQuery.newQuery().name("World Gate").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            if (Interfaces.isOpen(847)) {
                Execution.delay(RandomGenerator.nextInt(1000, 2000));
                MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 55509014);
                Execution.delay(RandomGenerator.nextInt(5000, 7500));
                navigateTo(crystalShapeshifterCoords);
                println("Navigating to Crystal Shapeshifter");
            }
        } else {
            Npc shapeshifter = shapeshifterResults.nearest();
            if (!getLocalPlayer().hasTarget() && shapeshifter != null && shapeshifter.interact("Attack")) {
                println("Attacking Crystal Shapeshifter");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void NodonDragonkin() {
        if (getLocalPlayer() == null) {
            return;
        }
        HandleSoulSplit();
        KeepHydrated();
        Coordinate nodonDragonkinCoords = new Coordinate(1679, 1310, 0);

        EntityResultSet<Npc> nodonDragonkinResults = NpcQuery.newQuery().name("Nodon engineer").results();
        EntityResultSet<Npc> siegeEnginesResults = NpcQuery.newQuery().name("Siege engine").results();
        EntityResultSet<Npc> nodonArtificerResults = NpcQuery.newQuery().name("Nodon artificer").results();
        EntityResultSet<Npc> nodonGuardResults = NpcQuery.newQuery().name("Nodon guard").results();
        EntityResultSet<Npc> nodonHunterResults = NpcQuery.newQuery().name("Nodon hunter").results();

        List<Npc> allNpcs = Stream.of(nodonDragonkinResults, siegeEnginesResults, nodonArtificerResults, nodonGuardResults, nodonHunterResults)
                .flatMap(EntityResultSet::stream)
                .collect(Collectors.toList());

        Npc nearestNpc = allNpcs.stream()
                .filter(npc -> Distance.between(nodonDragonkinCoords, npc.getCoordinate()) <= 15)
                .min(Comparator.comparing(npc -> Distance.between(Client.getLocalPlayer().getCoordinate(), npc.getCoordinate())))
                .orElse(null);

        if (nearestNpc == null) {
            navigateTo(nodonDragonkinCoords);
            println("Navigating to Nodon Dragonkin area or no nearby targets within 18 tiles.");
        } else {
            if (!getLocalPlayer().hasTarget() && nearestNpc.interact("Attack")) {
                println("Attacking " + nearestNpc.getName());
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void SoulDevourers() {
        if (getLocalPlayer() == null) {
            return;
        }
        KeepHydrated();
        ActivateMeleePrayer();
        Coordinate soulDevourersCoords = new Coordinate(3290, 2708, 0);
        Coordinate checkpoints1 = new Coordinate(2405, 6856, 3);
        Coordinate checkpoints2 = new Coordinate(2440, 6869, 1);

        EntityResultSet<Npc> salawaAkhResults = NpcQuery.newQuery().name("Salawa akh").results();
        if (salawaAkhResults.isEmpty()) {
            navigateTo(soulDevourersCoords);
            if (SceneObjectQuery.newQuery().name("Dungeon entrance").results().nearest().interact("Enter")) {
                Execution.delay(RandomGenerator.nextInt(5000, 7500));
                navigateTo(checkpoints1);
                if (SceneObjectQuery.newQuery().name("Rope").results().nearest().interact("Climb down")) {
                    Execution.delay(RandomGenerator.nextInt(5000, 7500));
                    navigateTo(checkpoints2);
                }
            }
        } else {
            Npc salawaAkh = salawaAkhResults.nearest();
            if (!getLocalPlayer().hasTarget() && salawaAkh != null && salawaAkh.interact("Attack")) {
                println("Attacking Salawa akh");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void Dinosaur() {
        if (getLocalPlayer() == null) {
            return;
        }
        ActivateMeleePrayer();
        KeepHydrated();
        Coordinate dinosaurCoords = new Coordinate(5434, 2532, 0);

        EntityResultSet<Npc> dinosaurResults = NpcQuery.newQuery().name("Venomous dinosaur").results();
        if (dinosaurResults.isEmpty()) {
            println("Navigating to Venomous Dinosaur");
            navigateTo(dinosaurCoords);
        } else {
            Npc dinosaur = dinosaurResults.nearest();
            if (!getLocalPlayer().hasTarget() && dinosaur != null && dinosaur.interact("Attack")) {
                println("Attacking Venomous Dinosaur");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void MithrilDragons() {
        if (getLocalPlayer() == null) {
            return;
        }
        ActivateMagicPrayer();
        KeepHydrated();
        Coordinate Stairs = new Coordinate(1694, 5296, 1);
        Coordinate CaveEntrance = new Coordinate(1778, 5346, 0);
        Coordinate MithrilDragonCoords = new Coordinate(1765, 5337, 1);

        EntityResultSet<Npc> mithrilDragon = NpcQuery.newQuery().name("Mithril dragon").results();
        if (mithrilDragon.isEmpty()) {
            println("Navigating to Mithril Dragons");
            navigateTo(Stairs);
            SceneObjectQuery.newQuery().name("Rough hewn steps").results().nearest().interact("Climb-down");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(CaveEntrance);
            SceneObjectQuery.newQuery().name("Stairs").results().nearest().interact("Climb-up");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(MithrilDragonCoords);

        } else {
            Npc dinosaur = mithrilDragon.nearest();
            if (!getLocalPlayer().hasTarget() && dinosaur != null && dinosaur.interact("Attack")) {
                println("Attacking Mithril Dragons");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void Demons() {
        if (getLocalPlayer() == null) {
            return;
        }
        HandleSoulSplit();
        KeepHydrated();
        Coordinate abyssalDemonCoords = new Coordinate(3230, 3654, 0);

        EntityResultSet<Npc> abyssalDemonResults = NpcQuery.newQuery().name("Abyssal demon").results();
        if (abyssalDemonResults.isEmpty()) {
            println("Navigating to Abyssal Demon");
            navigateTo(abyssalDemonCoords);
        } else {
            Npc abyssalDemon = abyssalDemonResults.nearest();
            if (!getLocalPlayer().hasTarget() && abyssalDemon != null && abyssalDemon.interact("Attack")) {
                println("Attacking Abyssal Demon");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void OrderOfAscension() {
        if (getLocalPlayer() == null) {
            return;
        }
        HandleSoulSplit();
        KeepHydrated();

        Coordinate rorariusCoords = new Coordinate(2501, 2886, 0);
        Coordinate rorariusEntranceCoords = new Coordinate(1109, 595, 1);

        EntityResultSet<Npc> rorariusResults = NpcQuery.newQuery().name("Rorarius").results();
        if (rorariusResults.isEmpty()) {
            println("Navigating to Rorarius");
            navigateTo(rorariusCoords);
            SceneObjectQuery.newQuery().name("Ascension dungeon").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(rorariusEntranceCoords);
        } else {
            Npc rorarius = rorariusResults.nearest();
            if (!getLocalPlayer().hasTarget() && rorarius != null && rorarius.interact("Attack")) {
                println("Attacking Rorarius");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void Kalphites() {
        if (getLocalPlayer() == null) {
            return;
        }
        ActivateMagicPrayer();
        KeepHydrated();

        Coordinate kaphiteCoords = new Coordinate(3234, 2858, 0);
        Coordinate kaphiteEntranceCoords = new Coordinate(2994, 1622, 0);

        EntityResultSet<Npc> kaphiteResults = NpcQuery.newQuery().name("Exiled kalphite marauder", "Exiled kalphite guardian").results();
        if (kaphiteResults.isEmpty()) {
            println("Navigating to Kaphites");
            navigateTo(kaphiteCoords);
            SceneObjectQuery.newQuery().name("Kalphite entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(kaphiteEntranceCoords);
        } else {
            Npc kaphite = kaphiteResults.nearest();
            if (!getLocalPlayer().hasTarget() && kaphite != null && kaphite.interact("Attack")) {
                println("Attacking Kaphite");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void Elves() {
        if (getLocalPlayer() == null) {
            return;
        }
        HandleSoulSplit();
        KeepHydrated();

        Coordinate Iorwoth = new Coordinate(2193, 3325, 1);
        Pattern pattern = Pattern.compile("Iorwerth", Pattern.CASE_INSENSITIVE);

        EntityResultSet<Npc> iorwerthElves = NpcQuery.newQuery().option("Attack").results();

        if (iorwerthElves.isEmpty()) {
            println("Navigating to Iorwoth Elves");
            navigateTo(Iorwoth);
        } else {
            Npc nearestNpc = null;
            for (Npc npc : iorwerthElves) {
                if (pattern.matcher(npc.getName()).find()) {
                    nearestNpc = npc;
                    break;
                }
            }

            if (!getLocalPlayer().hasTarget() && nearestNpc != null && nearestNpc.interact("Attack")) {
                println("Attacking: " + nearestNpc.getName());
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void ShadowCreatures() {
        if (getLocalPlayer() == null) {
            return;
        }
        HandleSoulSplit();
        KeepHydrated();

        Coordinate ShadowGate = new Coordinate(2171, 3366, 1);
        Coordinate Shadowinside = new Coordinate(2167, 3383, 1);

        EntityResultSet<Npc> manifestShadows = NpcQuery.newQuery().name("Manifest shadow").results();
        EntityResultSet<Npc> blissfulShadows = NpcQuery.newQuery().name("Blissful shadow").results();
        EntityResultSet<Npc> truthfulShadows = NpcQuery.newQuery().name("Truthful shadow").results();

        if (manifestShadows.isEmpty() && blissfulShadows.isEmpty() && truthfulShadows.isEmpty()) {
            println("Navigating to Shadow Creatures");
            navigateTo(ShadowGate);
            if (SceneObjectQuery.newQuery().name("Barrier").results().nearest().interact("Enter")) {
                Execution.delayUntil(RandomGenerator.nextInt(5000, 7500), () -> SceneObjectQuery.newQuery().name("Barrier").results().isEmpty()); // Wait for barrier interaction to complete
                navigateTo(Shadowinside);
            }
        } else {

            Npc targetShadow = !manifestShadows.isEmpty() ? manifestShadows.nearest() :
                    !blissfulShadows.isEmpty() ? blissfulShadows.nearest() :
                            truthfulShadows.nearest();

            if (!getLocalPlayer().hasTarget() && targetShadow != null && targetShadow.interact("Attack")) {
                println("Attacking Shadow Creatures: " + targetShadow.getName());
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void VileBlooms() {
        if (getLocalPlayer() == null) {
            return;
        }
        ActivateMagicPrayer();
        KeepHydrated();

        int playerSlayerLevel = Skills.SLAYER.getSkill().getLevel();

        // NPC names and their required Slayer levels
        String[] npcNames = {"Devil's snare", "Luminous snaggler", "Lampenflora", "Liverworts"};
        int[] slayerRequirements = {90, 95, 102, 110};
        Coordinate[] npcCoordinates = {
                new Coordinate(5600, 2124, 0), // Devil's snare
                new Coordinate(5284, 2387, 0), // Luminous snaggler
                new Coordinate(5617, 2262, 0), // Lampenflora
                new Coordinate(5680, 2342, 0)  // Liverworts
        };

        // Determine the highest-level NPC that the player can attack based on Slayer level
        String targetNpcName = null;
        Coordinate targetNpcCoords = null;
        for (int i = npcNames.length - 1; i >= 0; i--) {
            if (playerSlayerLevel >= slayerRequirements[i]) {
                targetNpcName = npcNames[i];
                targetNpcCoords = npcCoordinates[i];
                break;
            }
        }

        if (targetNpcName == null) {
            println("No NPCs available for your Slayer level.");
            return;
        }

        // Now that we have the highest-level NPC, let's find and attack it
        EntityResultSet<Npc> targetNpcs = NpcQuery.newQuery().name(targetNpcName).results();
        if (targetNpcs.isEmpty()) {
            println("Navigating to " + targetNpcName);
            navigateTo(targetNpcCoords);
        } else {
            Npc targetNpc = targetNpcs.nearest();
            if (!getLocalPlayer().hasTarget() && targetNpc != null && targetNpc.interact("Attack")) {
                println("Attacking " + targetNpcName);
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void IceStrykewyrms() {
        if (getLocalPlayer() == null) {
            return;
        }

        Coordinate baseCoordinate = new Coordinate(3062, 3800, 0);

        int[] npcIds = {19476, 19477, 19478, 19479};

        for (int npcId : npcIds) {
            EntityResultSet<Npc> mounds = NpcQuery.newQuery().id(npcId).results();

            if (mounds.isEmpty()) {
                RandomGenerator random = new RandomGenerator();
                int xOffset = random.nextInt(-5, 5);
                int yOffset = random.nextInt(-5, 5);
                Coordinate randomCoordinate = baseCoordinate.derive(xOffset, yOffset, 0);

                println("Navigating to a random point near Ice Strykewyrm mounds");
                navigateTo(randomCoordinate);
            } else {
                HandleSoulSplit();
                KeepHydrated();
                Npc mound = mounds.nearest();
                if (mound != null && mound.interact("Investigate")) {
                    println("Investigating mound");
                    Execution.delayUntil(RandomGenerator.nextInt(5000, 10000), () -> {
                        Npc strykewyrm = NpcQuery.newQuery().name("Ice strykewyrm").results().nearest();
                        return strykewyrm != null && strykewyrm.getOptions().contains("Attack");
                    });

                    Npc strykewyrm = NpcQuery.newQuery().name("Ice strykewyrm").results().nearest();
                    if (!getLocalPlayer().hasTarget() && strykewyrm != null && strykewyrm.interact("Attack")) {
                        println("Attacking Ice Strykewyrm");
                        Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                            PathingEntity<?> target = getLocalPlayer().getTarget();
                            return target == null || target.getCurrentHealth() == 0;
                        });
                    }
                }

                Execution.delay(RandomGenerator.nextInt(601, 998));
            }
        }
    }

    private void LavaStrykewyrms() {
        if (getLocalPlayer() == null) {
            return;
        }

        int[] npcIds = {19466, 19467, 19468, 19469, 19470, 19471, 19472, 19473, 19474, 19475, 19480};
        List<Npc> validNpcs = new ArrayList<>();

        EntityResultSet<Npc> allNpcs = NpcQuery.newQuery().results();
        for (Npc npc : allNpcs) {
            for (int id : npcIds) {
                if (npc.getId() == id) {
                    validNpcs.add(npc);
                    break;
                }
            }
        }

        if (validNpcs.isEmpty()) {
            println("Navigating to Lava Strykewyrm mounds");
            navigateTo(new Coordinate(3030, 3827, 0));
        } else {

            Npc nearestMound = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Npc npc : validNpcs) {
                double distance = Distance.between(getLocalPlayer().getCoordinate(), npc.getCoordinate());
                if (distance < nearestDistance) {
                    nearestMound = npc;
                    nearestDistance = distance;
                }
            }

            if (nearestMound != null && nearestMound.interact("Investigate")) {
                println("Investigating nearest mound");

                Execution.delayUntil(RandomGenerator.nextInt(5000, 10000), () -> {
                    Npc strykewyrm = NpcQuery.newQuery().name("Lava strykewyrm").results().nearestTo(getLocalPlayer());
                    return strykewyrm != null && strykewyrm.getOptions().contains("Attack");
                });

                HandleSoulSplit();
                KeepHydrated();

                Npc strykewyrm = NpcQuery.newQuery().name("Lava strykewyrm").results().nearestTo(getLocalPlayer());
                if (!getLocalPlayer().hasTarget() && strykewyrm != null && strykewyrm.interact("Attack")) {
                    println("Attacking nearest Lava Strykewyrm");
                    Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                        PathingEntity<?> target = getLocalPlayer().getTarget();
                        return target == null || target.getCurrentHealth() == 0;
                    });
                }
            }
            Execution.delay(RandomGenerator.nextInt(601, 998));
        }
    }

    private void GreaterDemons() {
        if (getLocalPlayer() == null) {
            return;
        }
        HandleSoulSplit();
        KeepHydrated();

        Coordinate greaterDemonsCoords = new Coordinate(1686, 5288, 1);
        Coordinate greaterDemonsEntranceCoords = new Coordinate(1636, 5253, 0);


        EntityResultSet<Npc> greaterDemonResults = NpcQuery.newQuery().name("Greater demon").results();
        if (greaterDemonResults.isEmpty()) {
            println("Navigating to Greater Demons");
            navigateTo(greaterDemonsCoords);
            SceneObjectQuery.newQuery().name("Cave").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(greaterDemonsEntranceCoords);
            SceneObjectQuery.newQuery().name("Energy barrier").results().nearest().interact("Pass");
        } else {
            Npc greaterDemon = greaterDemonResults.nearest();
            if (!getLocalPlayer().hasTarget() && greaterDemon != null && greaterDemon.interact("Attack")) {
                println("Attacking Greater Demon");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void MutatedJadinkos() {
        if (getLocalPlayer() == null) {
            return;
        }
        HandleSoulSplit();
        KeepHydrated();

        Coordinate Jadinkos = new Coordinate(2944, 2956, 0);

        EntityResultSet<Npc> MutatedJadinko = NpcQuery.newQuery().name("Mutated jadinko male").results();
        if (MutatedJadinko.isEmpty()) {
            println("Navigating to Mutated Jadinkos");
            navigateTo(Jadinkos);
            SceneObjectQuery.newQuery().name("Hole").results().nearest().interact("Squeeze-through");
            Execution.delay(RandomGenerator.nextInt(10000, 12500));
            Movement.walkTo(3059, 9241, false);
            Execution.delay(RandomGenerator.nextInt(10000, 12500));
        } else {
            Npc greaterDemon = MutatedJadinko.nearest();
            if (!getLocalPlayer().hasTarget() && greaterDemon != null && greaterDemon.interact("Attack")) {
                println("Attacking Mutated Jadinko");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }

        Execution.delay(RandomGenerator.nextInt(601, 998));
    }

    private void CorruptedCreatures() {
        if (Client.getLocalPlayer() == null) {
            println("No local player found.");
            return;
        }

        int playerSlayerLevel = Skills.SLAYER.getSkill().getLevel();

        if (playerSlayerLevel >= 103) {
            handleCorruptedWorker();
        } else if (playerSlayerLevel >= 100) {
            handleCorruptedKalphite();
        } else if (playerSlayerLevel >= 97) {
            handleCorruptedDustDevil();
        } else if (playerSlayerLevel >= 94) {
            handleCorruptedLizard();
        } else if (playerSlayerLevel >= 91) {
            handleCorruptedScarab();
        } else if (playerSlayerLevel >= 88) {
            handleCorruptedScorpion();
        } else {
            println("Your Slayer level is not high enough to attack any corrupted creatures.");
        }
    }

    private void handleCorruptedScorpion() {
        if (getLocalPlayer() == null) {
            return;
        }

        Coordinate DungeonEntrance = new Coordinate(3291, 2708, 0);
        Coordinate corruptedScorpionCoords = new Coordinate(2384, 6818, 3);


        EntityResultSet<Npc> CorruptedScorpion = NpcQuery.newQuery().name("Corrupted scorpion").results();
        if (CorruptedScorpion.isEmpty()) {
            println("Navigating to Corrupted Scorpion");
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Dungeon entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(corruptedScorpionCoords);
        } else {
            ActivateMeleePrayer();
            KeepHydrated();
            Npc Scorpion = CorruptedScorpion.nearest();
            if (!getLocalPlayer().hasTarget() && Scorpion != null && Scorpion.interact("Attack")) {
                println("Attacking Corrupted Scorpion");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void handleCorruptedScarab() {
        if (getLocalPlayer() == null) {
            return;
        }

        Coordinate DungeonEntrance = new Coordinate(3291, 2708, 0);
        Coordinate corruptedScarabCoords = new Coordinate(2410, 6813, 3);


        EntityResultSet<Npc> CorruptedScarab = NpcQuery.newQuery().name("Corrupted scarab").results();
        if (CorruptedScarab.isEmpty()) {
            println("Navigating to Corrupted Scarab");
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Dungeon entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(corruptedScarabCoords);
        } else {
            ActivateMeleePrayer();
            KeepHydrated();
            Npc Scarab = CorruptedScarab.nearest();
            if (!getLocalPlayer().hasTarget() && Scarab != null && Scarab.interact("Attack")) {
                println("Attacking Corrupted Scarab");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void handleCorruptedLizard() {
        if (getLocalPlayer() == null) {
            return;
        }

        Coordinate DungeonEntrance = new Coordinate(3291, 2708, 0);
        Coordinate corruptedLizardCoords = new Coordinate(2405, 6843, 3);


        EntityResultSet<Npc> CorruptedLizard = NpcQuery.newQuery().name("Corrupted lizard").results();
        if (CorruptedLizard.isEmpty()) {
            println("Navigating to Corrupted Lizard");
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Dungeon entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(corruptedLizardCoords);
        } else {
            ActivateRangedPrayer();
            KeepHydrated();
            Npc Lizard = CorruptedLizard.nearest();
            if (!getLocalPlayer().hasTarget() && Lizard != null && Lizard.interact("Attack")) {
                println("Attacking Corrupted Lizard");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void handleCorruptedDustDevil() {
        if (getLocalPlayer() == null) {
            return;
        }
        Coordinate DungeonEntrance = new Coordinate(3290, 2708, 0);
        Coordinate Rope = new Coordinate(2405, 6856, 3);
        Coordinate DustDevils = new Coordinate(2387, 6896, 1);

        EntityResultSet<Npc> CorruptedDustDevil = NpcQuery.newQuery().name("Corrupted dust devil").results();
        if (CorruptedDustDevil.isEmpty()) {
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Dungeon entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Rope);
            SceneObjectQuery.newQuery().name("Rope").results().nearest().interact("Climb down");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(DustDevils);
        } else {
            KeepHydrated();
            HandleSoulSplit();
            Npc DustDevil = CorruptedDustDevil.nearest();
            if (!getLocalPlayer().hasTarget() && DustDevil != null && DustDevil.interact("Attack")) {
                println("Attacking Corrupted Dust Devil");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void handleCorruptedKalphite() {
        if (getLocalPlayer() == null) {
            return;
        }
        Coordinate DungeonEntrance = new Coordinate(3290, 2708, 0);
        Coordinate Rope = new Coordinate(2405, 6856, 3);
        Coordinate Kalphite = new Coordinate(2438, 6896, 1);

        EntityResultSet<Npc> CorruptedKalphite = NpcQuery.newQuery().name("Corrupted kalphite marauder", "Corrupted kalphite guardian").results();
        if (CorruptedKalphite.isEmpty()) {
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Dungeon entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Rope);
            SceneObjectQuery.newQuery().name("Rope").results().nearest().interact("Climb down");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Kalphite);
        } else {
            KeepHydrated();
            ActivateMagicPrayer();
            Npc CorruptedMarauder = CorruptedKalphite.nearest();
            if (!getLocalPlayer().hasTarget() && CorruptedMarauder != null && CorruptedMarauder.interact("Attack")) {
                println("Attacking Corrupted Kalphite");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void handleCorruptedWorker() {
        if (getLocalPlayer() == null) {
            return;
        }
        Coordinate DungeonEntrance = new Coordinate(3290, 2708, 0);
        Coordinate Rope = new Coordinate(2405, 6856, 3);
        Coordinate Worker = new Coordinate(2476, 6877, 1);

        EntityResultSet<Npc> CorruptedWorker = NpcQuery.newQuery().name("Corrupted worker").results();
        if (CorruptedWorker.isEmpty()) {
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Dungeon entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Rope);
            SceneObjectQuery.newQuery().name("Rope").results().nearest().interact("Climb down");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Worker);
        } else {
            KeepHydrated();
            ActivateMeleePrayer();
            Npc GreenWorker = CorruptedWorker.nearest();
            if (!getLocalPlayer().hasTarget() && GreenWorker != null && GreenWorker.interact("Attack")) {
                println("Attacking Corrupted Worker");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void IronDragons() {
        if (getLocalPlayer() == null) {
            return;
        }
        Coordinate DungeonEntrance = new Coordinate(2745, 3152, 0);
        Coordinate Gap = new Coordinate(2722, 9456, 0);

        EntityResultSet<Npc> IronDragon = NpcQuery.newQuery().name("Iron dragon").results();
        if (IronDragon.isEmpty()) {
            navigateTo(DungeonEntrance);
            NpcQuery.newQuery().name("Saniboch").results().nearest().interact("Pay");
            Execution.delay(RandomGenerator.nextInt(1000, 2000));
            SceneObjectQuery.newQuery().name("Dungeon entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Gap);
        } else {
            KeepHydrated();
            ActivateMagicPrayer();
            Npc Dragons = IronDragon.nearest();
            if (!getLocalPlayer().hasTarget() && Dragons != null && Dragons.interact("Attack")) {
                println("Attacking Iron Dragon");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void AdamantDragons() {
        if (getLocalPlayer() == null) {
            return;
        }
        KeepHydrated();
        ActivateMagicPrayer();
        Coordinate DungeonEntrance = new Coordinate(2745, 3152, 0);
        Coordinate Gap = new Coordinate(4512, 6032, 0);

        EntityResultSet<Npc> AdamantDragon = NpcQuery.newQuery().name("Adamant dragon").results();
        if (AdamantDragon.isEmpty()) {
            navigateTo(DungeonEntrance);
            NpcQuery.newQuery().name("Saniboch").results().nearest().interact("Pay");
            Execution.delay(RandomGenerator.nextInt(1000, 2000));
            SceneObjectQuery.newQuery().name("Dungeon entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Gap);
        } else {
            Npc AddyDragons = AdamantDragon.nearest();
            if (!getLocalPlayer().hasTarget() && AddyDragons != null && AddyDragons.interact("Attack")) {
                println("Attacking Adamant Dragon");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void BlackDemons() {
        if (getLocalPlayer() == null) {
            return;
        }
        KeepHydrated();
        ActivateMagicPrayer();
        Coordinate DungeonEntrance = new Coordinate(2866, 9779, 0);

        EntityResultSet<Npc> BlackDemons = NpcQuery.newQuery().name("Black demon").results();
        if (BlackDemons.isEmpty()) {
            navigateTo(DungeonEntrance);
        } else {
            Npc Demons = BlackDemons.nearest();
            if (!getLocalPlayer().hasTarget() && Demons != null && Demons.interact("Attack")) {
                println("Attacking Black Demon");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void KalgerionDemons() {
        if (getLocalPlayer() == null) {
            return;
        }
        KeepHydrated();
        HandleSoulSplit();
        Coordinate DungeonEntrance = new Coordinate(3401, 3665, 0);

        EntityResultSet<Npc> KalgarianDemons = NpcQuery.newQuery().name("Kal'gerion demon").results();
        if (KalgarianDemons.isEmpty()) {
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Mysterious entrance").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
        } else {
            Npc Demons = KalgarianDemons.nearest();
            if (!getLocalPlayer().hasTarget() && Demons != null && Demons.interact("Attack")) {
                println("Attacking Kal'gerion Demon");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void Gargoyle() {
        if (getLocalPlayer() == null) {
            return;
        }
        Coordinate DungeonEntrance = new Coordinate(1685, 5287, 1);
        Coordinate Barrier1 = new Coordinate(1635, 5253, 0);
        Coordinate Barrier2 = new Coordinate(1605, 5264, 0);
        Coordinate Barrier3 = new Coordinate(1610, 5288, 0);

        EntityResultSet<Npc> Gars = NpcQuery.newQuery().name("Gargoyle").results();
        if (Gars.isEmpty()) {
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Cave").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(1000, 2000));
            navigateTo(Barrier1);
            SceneObjectQuery.newQuery().name("Energy barrier").results().nearest().interact("Pass");
            Execution.delay(RandomGenerator.nextInt(1000, 2000));
            navigateTo(Barrier2);
            SceneObjectQuery.newQuery().name("Energy barrier").results().nearest().interact("Pass");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(Barrier3);
            SceneObjectQuery.newQuery().name("Energy barrier").results().nearest().interact("Pass");

        } else {
            KeepHydrated();
            ActivateMagicPrayer();
            Npc Gargoyles = Gars.nearest();
            if (!getLocalPlayer().hasTarget() && Gargoyles != null && Gargoyles.interact("Attack")) {
                println("Attacking Gargoyle");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
        }
    }

    private void RipperDemons() {
        if (getLocalPlayer() == null) {
            return;
        }
        Coordinate DungeonEntrance = new Coordinate(3351, 3148, 0);
        Coordinate RipperDemons = new Coordinate(5139, 7582, 0);

        EntityResultSet<Npc> Ripper = NpcQuery.newQuery().name("Ripper Demon").results();
        if (Ripper.isEmpty()) {
            navigateTo(DungeonEntrance);
            SceneObjectQuery.newQuery().name("Dark Tunnel").results().nearest().interact("Enter");
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            navigateTo(RipperDemons);
        } else {
            Npc Rippers = Ripper.nearest();
            if (!getLocalPlayer().hasTarget() && Rippers != null && Rippers.interact("Attack")) {
                KeepHydrated();
                ActivateMeleePrayer();
                println("Attacking Ripper Demon");
                Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> {
                    PathingEntity<?> target = getLocalPlayer().getTarget();
                    return target == null || target.getCurrentHealth() == 0;
                });
            }
            if (Rippers.getCurrentHealth() < 2000 || Rippers.getAnimationId() == 27775) {
                Coordinate npcCoord = Rippers.getCoordinate();
                if (npcCoord != null) {
                    int[][] directions = {
                            {0, 4},
                            {4, 0},
                            {0, -4},
                            {-4, 0}
                    };

                    for (int[] dir : directions) {
                        Coordinate targetCoord = new Coordinate(npcCoord.getX() + dir[0], npcCoord.getY() + dir[1], npcCoord.getZ());

                        if (targetCoord.isReachable()) {
                            Movement.walkTo(targetCoord.getX(), targetCoord.getY(), true);
                            break;
                        }
                    }
                }
            }
        }
    }






    public void DeathsOffice() {
        interactWithDeath();
    }

    private void interactWithDeath() {
        Npc death = NpcQuery.newQuery().name("Death").results().nearest();
        if (death == null) {
            return;
        }

        println("Attempting to interact with Death.");
        Execution.delay(RandomGenerator.nextInt(3500, 5000));
        if (death.interact("Reclaim items")) {
            println("Interaction initiated. Waiting for interface 1626 to open.");
            if (Execution.delayUntil(5000, () -> Interfaces.isOpen(1626))) {
                println("Successfully opened interface 1626. Moving to reclaim confirmation.");
                Execution.delay(RandomGenerator.nextInt(3500, 5000));
                confirmReclaim(); // Proceed to confirm reclaim if successful.
            } else {
                println("Failed to open interface 1626 after interacting with Death.");
            }
        } else {
            println("Failed to initiate interaction with Death.");
        }
    }

    private void confirmReclaim() {
        if (!Interfaces.isOpen(1626)) {
            println("Interface 1626 is not open. Cannot confirm reclaim.");
            return;
        }

        ComponentQuery query = ComponentQuery.newQuery(1626);
        List<Component> components = query.componentIndex(47).results().stream().toList();
        if (!components.isEmpty() && components.get(0).interact(1)) {
            println("Reclaim confirmation initiated. Waiting for finalization option.");
            Execution.delay(RandomGenerator.nextInt(3500, 5000));
            finalizeReclamation();
        } else {
            println("Failed to confirm reclaim with Death.");
        }
    }

    private void finalizeReclamation() {
        if (!Interfaces.isOpen(1626)) {
            println("Interface 1626 is not open. Cannot finalize reclaim.");
            return;
        }

        ComponentQuery query = ComponentQuery.newQuery(1626);
        List<Component> components = query.componentIndex(72).results().stream().toList();
        if (!components.isEmpty() && components.get(0).interact(1)) {
            println("Reclaim finalized. Moving to post-reclaim actions.");
            Execution.delay(RandomGenerator.nextInt(3500, 5000));
            botState = BotState.WARS_RETREAT;
        } else {
            println("Failed to finalize reclaim with Death.");
        }
    }
    private void DeActivateMagicPrayer() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(16768) != 0 && getLocalPlayer().inCombat()) {
                ActionBar.useAbility("Deflect Magic");
            }
        }
    }

    private void DeActivateRangedPrayer() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(16769) != 0 && getLocalPlayer().inCombat()) {
                ActionBar.useAbility("Deflect Ranged");
            }
        }
    }

    private void DeActivateMeleePrayer() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(16770) != 0 && getLocalPlayer().inCombat()) {
                ActionBar.useAbility("Deflect Melee");
            }
        }
    }

    private void DeHandleSoulSplit() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(16779) != 0 && getLocalPlayer().inCombat()) {
                ActionBar.useAbility("Soul Split");
            }
        }
    }


    private void ActivateMagicPrayer() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(16768) != 1 && getLocalPlayer().inCombat()) {
                ActionBar.useAbility("Deflect Magic");
            }
        }
    }

    private void ActivateRangedPrayer() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(16769) != 1 && getLocalPlayer().inCombat()) {
                ActionBar.useAbility("Deflect Ranged");
            }
        }
    }

    private void ActivateMeleePrayer() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(16770) != 1 && getLocalPlayer().inCombat()) {
                ActionBar.useAbility("Deflect Melee");
            }
        }
    }

    private void HandleSoulSplit() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(16779) != 1 && getLocalPlayer().inCombat()) {
                ActionBar.useAbility("Soul Split");
            }
        }
    }

    private void KeepHydrated() {
        drinkOverloads();
        usePrayerOrRestorePots();
        eatFood();
    }

    public void drinkOverloads() {
        if (getLocalPlayer() != null && VarManager.getVarbitValue(26037) == 0) {
            if (getLocalPlayer().inCombat()) {

                ResultSet<Item> items = InventoryItemQuery.newQuery().results();

                Item overloadPot = items.stream()
                        .filter(item -> item.getName() != null && overloads.matcher(item.getName()).find())
                        .findFirst()
                        .orElse(null);

                if (overloadPot != null) {
                    println("Drinking " + overloadPot.getName());
                    Backpack.interact(overloadPot.getName(), "Drink");
                    Execution.delay(RandomGenerator.nextInt(1180, 1220));
                }
            }
        }
    }

    public void usePrayerOrRestorePots() {
        if (getLocalPlayer() != null) {
            if (getLocalPlayer().inCombat()) {
                int randomPrayerThreshold = RandomGenerator.nextInt(900, 2500);
                if (getLocalPlayer().getPrayerPoints() < randomPrayerThreshold) {
                    ResultSet<Item> items = InventoryItemQuery.newQuery().results();
                    Pattern pattern = PrayerPotion;
                    Item prayerOrRestorePot = items.stream()
                            .filter(item -> item.getName() != null && pattern.matcher(item.getName()).find())
                            .findFirst()
                            .orElse(null);

                    if (prayerOrRestorePot != null) {
                        println("Drinking " + prayerOrRestorePot.getName());
                        boolean success = Backpack.interact(prayerOrRestorePot.getName(), "Drink");

                        if (success) {
                            println(prayerOrRestorePot.getName() + " has been used.");
                            Execution.delay(RandomGenerator.nextInt(1180, 1220));

                        } else {
                            println("Failed to use " + prayerOrRestorePot.getName());
                        }
                    } else {
                        println("No Prayer or Restore pots found.");
                        botState = BotState.WARS_RETREAT;
                    }
                }
            }
        }
    }

    public void eatFood() {
        if (getLocalPlayer() != null) {
            if (getLocalPlayer().getAnimationId() == 18001)
                return;

            int currentHealth = getLocalPlayer().getCurrentHealth();
            int maximumHealth = getLocalPlayer().getMaximumHealth();

            int healthPercentage = currentHealth * 100 / maximumHealth;
            int randomHealthThreshold = RandomGenerator.nextInt(40, 60); // Generates a random threshold between 40% and 60%

            if (healthPercentage < randomHealthThreshold) {
                ResultSet<Item> foodItems = InventoryItemQuery.newQuery(93).option("Eat").results();

                if (!foodItems.isEmpty()) {
                    Item food = foodItems.first();
                    if (food != null) {
                        println("Attempting to eat " + food.getName());
                        boolean success = Backpack.interact(food.getName(), 1);
                        if (success) {
                            println("Eating " + food.getName());
                            Execution.delay(RandomGenerator.nextInt(1780, 1820));
                        } else {
                            println("Failed to eat " + food.getName());
                        }
                    }
                } else {
                    println("No food found!");
                    botState = BotState.WARS_RETREAT;
                }
            }
        }
    }
}