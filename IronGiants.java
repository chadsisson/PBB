package com.scripts.practice;
 
import org.powerbot.script.*;
import org.powerbot.script.Random;
import org.powerbot.script.rt6.*;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.Component;
 
 
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.*;
 
 
/**
 * Created by Chad Sisson on 11/13/2014
 */
@Script.Manifest(name = "IronGiants", description = "Kills Hill giants and loots the bones")
public class IronGaints extends PollingScript<ClientContext> implements PaintListener, MessageListener {
 
    private int[] cowIds = { x, x, x };
    private Tile[] pathToCows = { new Tile(2891, 3502), new Tile(2891, 3494), new Tile(2885, 3494), new Tile(2885, 3495), new Tile(2884, 3495), new Tile(2884, 3492) };
    private Tile[] pathToJack = { new Tile(2884, 3492), new Tile(2884, 3495), new Tile(2885, 3495), new Tile(2885, 3494), new Tile(2891, 3494), new Tile(2891, 3502), new Tile(2887, 3502) };
    private Tile[] pathToBank = { new Tile(xxxx, xxxx), new Tile(xxxx, xxxx) };
 
    private int GaintsKilled = 0;
    private int BigBonesLooted = 0;
    private int hidesTanned = 0;
    private int bootsMade = 0;
    private int moneyMade = 0;
    private long startTime = System.currentTimeMillis();
    private String currentTask = "";
 
    private TilePath path_ToCows = ctx.movement.newTilePath(pathToCows);
    private TilePath path_ToJack = ctx.movement.newTilePath(pathToJack);
 
    private final Player player = ctx.players.local();
 
    @Override
    public void poll() {
        final Npc npc = ctx.npcs.select().id(cowIds).nearest().poll();
        final Npc jackOval = ctx.npcs.select().id(14877).poll();
        final GroundItem item = ctx.groundItems.select().id(1739).nearest().poll();
        final State state = state();
        switch (state) {
            case ATTACK:
                if (npc.inViewport() && !npc.inCombat() && !player.inCombat()) {
                    currentTask = "Interacting with a Cow";
                    npc.interact("Attack");
                    //System.out.print("Attacking cow, ");
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            //System.out.print("waiting for cow to die, ");
                            currentTask = "In Combat";
                            return !player.inCombat() || !npc.valid();
                        }
                    });
                    //System.out.println("COW IS DEAD.");
                    GaintsKilled += 1;
                } else {
                    ctx.movement.step(npc);
                    ctx.camera.turnTo(npc);
                }
                break;
            case LOOT:
                if (item.inViewport()) {
                    currentTask = "Begin Looting";
                    item.interact("Take", "Big_Bones");
                   // System.out.print("Looting, ");
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            //System.out.print("picking up loot, ");
                            if (item.valid()) {
                                currentTask = "Picking up a " + item.name();
                            } else {
                                currentTask = "Waiting for next task";
                            }
                            return !item.valid();
                        }
                    });
                    //System.out.println("PLAYER HAS PICKED UP THE LOOT.");
                    BigBonesLooted += 1;
                } else {
                    ctx.movement.step(item);
                    ctx.camera.turnTo(item);
                }
                break;
            case BANK:
                if (item.inViewport()) {
                    currentTask = "Banking";
                    item.interact("Take", "Big_Bones");
                   // System.out.print("Looting, ");
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            //System.out.print("picking up loot, ");
                            if (item.valid()) {
                                currentTask = "Picking up a " + item.name();
                            } else {
                                currentTask = "Waiting for next task";
                            }
                            return !item.valid();
                        }
                    });
                    //System.out.println("PLAYER HAS PICKED UP THE LOOT.");
                    BigBonesLooted += 1;
                } else {
                    ctx.movement.step(item);
                    ctx.camera.turnTo(item);
                }
                break;
            case STOP:
                currentTask = "Stopping, not enough food to continue.";
                System.out.println("State.STOP");
                Condition.sleep(2000);
                ctx.controller.stop();
                break;
        }
    }
 
    public int perHour(int value) {
        return (int) ((value) * 3600000D / (System.currentTimeMillis() - startTime));
    }
 
    private final Font font = new Font("Arial", 1, 15);
    private final Font mainFont = new Font("Arial", 1, 12);
 
    @Override
    public void repaint(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(4, 390, 568, 206);
        g.setFont(font);
        g.setColor(Color.BLACK);
        g.drawString("IronGiants", 10, 410);
        g.setFont(mainFont);
        g.setColor(Color.WHITE);
        g.drawString("Time run: " + Timer.format(ctx.controller.script().getRuntime()), 10, 430);
        g.drawString("Current Task: " + currentTask, 284, 430);
        g.drawString("Hill Gaints Killed: " + GaintsKilled,10, 460);
        g.drawString("Killed/hr: " + perHour(cowsKilled), 284, 460);
        g.drawString("BigBones Looted: " + hidesGathered, 10, 490);
        g.drawString("Looted/hr: " + perHour(hidesGathered), 284, 490);
    }
 
 
    public State state() {
        if (!ctx.groundItems.id(1739).isEmpty() && ctx.backpack.select().count() < 28 && !player.interacting().valid()) {
            return State.LOOT;
        }
        if (ctx.backpack.select().count() == 28 && ctx.backpack.select().id(1739).count() > 0) {
            return State.BANK;
        }
        if (ctx.backpack.select().count() == 28 && !(ctx.backpack.select().id(1734).poll().stackSize() <= 5) && ctx.backpack.select().id(1743).count() > 0) {
            return State.CRAFT;
        }
        if (ctx.backpack.select().count() == 28 && ctx.backpack.select().id(25821).count() > 0) {
            return State.SELL;
        }
        if (ctx.backpack.select().id(1734).poll().stackSize() <= 5) {
            return State.STOP;
        }
        return State.ATTACK;
    }
 
    public enum State {
        ATTACK, LOOT,  TAN, CRAFT, SELL, STOP;
    }
}
