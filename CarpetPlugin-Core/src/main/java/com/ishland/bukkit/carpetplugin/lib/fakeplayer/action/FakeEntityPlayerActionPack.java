package com.ishland.bukkit.carpetplugin.lib.fakeplayer.action;

import com.google.common.base.Preconditions;
import com.ishland.bukkit.carpetplugin.lib.fakeplayer.base.FakeEntityPlayer;
import net.minecraft.server.BlockPosition;
import net.minecraft.server.Entity;
import net.minecraft.server.EnumDirection;
import net.minecraft.server.EnumHand;
import net.minecraft.server.EnumInteractionResult;
import net.minecraft.server.ItemStack;
import net.minecraft.server.MovingObjectPosition;
import net.minecraft.server.MovingObjectPositionBlock;
import net.minecraft.server.MovingObjectPositionEntity;
import net.minecraft.server.RayTrace;
import net.minecraft.server.Vec3D;
import net.minecraft.server.WorldServer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FakeEntityPlayerActionPack {

    public final FakeEntityPlayer fakeEntityPlayer;
    private final Set<Action> activeActions = new HashSet<>();

    public FakeEntityPlayerActionPack(FakeEntityPlayer fakeEntityPlayer) {
        Preconditions.checkNotNull(fakeEntityPlayer);
        this.fakeEntityPlayer = fakeEntityPlayer;
    }

    public void tick() {
        Iterator<Action> iterator = activeActions.iterator();
        while (iterator.hasNext()) {
            Action action = iterator.next();
            action.tick();
            if (action.isCompleted()) iterator.remove();
        }
    }

    public Set<Action> getActivatedActions() {
        return activeActions;
    }

    public void stopAll() {
        Iterator<Action> iterator = activeActions.iterator();
        while (iterator.hasNext()) {
            Action action = iterator.next();
            action.deactivate();
            iterator.remove();
        }
    }

    public void doSneak() {
        unSneak();
        unSprint(); // Conflict with sprint
        activeActions.add(new Action(ActionType.SNEAK, fakeEntityPlayer, 5, 1));
    }

    public void unSneak() {
        removeAll(ActionType.SNEAK);
    }

    public void doSprint() {
        unSprint();
        unSneak(); // Conflict with sneak
        activeActions.add(new Action(ActionType.SPRINT, fakeEntityPlayer, 5, 1));
    }

    public void unSprint() {
        removeAll(ActionType.SPRINT);
    }

    public void doUse() {
        unUse();
        activeActions.add(new Action(ActionType.USE, fakeEntityPlayer));
    }

    public void doUse(int interval, int repeats) {
        unUse();
        activeActions.add(new Action(ActionType.USE, fakeEntityPlayer, interval, repeats));
    }

    public void unUse() {
        removeAll(ActionType.USE);
    }

    public void dropOne() {
        unDrop();
        activeActions.add(new Action(ActionType.DROP_ONE, fakeEntityPlayer));
    }

    public void dropOne(int interval, int repeats) {
        unDrop();
        activeActions.add(new Action(ActionType.DROP_ONE, fakeEntityPlayer, interval, repeats));
    }

    public void dropStack() {
        unDrop();
        activeActions.add(new Action(ActionType.DROP_STACK, fakeEntityPlayer));
    }

    public void dropStack(int interval, int repeats) {
        unDrop();
        activeActions.add(new Action(ActionType.DROP_STACK, fakeEntityPlayer, interval, repeats));
    }

    public void dropAll() {
        unDrop();
        activeActions.add(new Action(ActionType.DROP_ALL, fakeEntityPlayer));
    }

    public void dropAll(int interval, int repeats) {
        unDrop();
        activeActions.add(new Action(ActionType.DROP_ALL, fakeEntityPlayer, interval, repeats));
    }

    public void unDrop() {
        removeAll(ActionType.DROP_ALL);
        removeAll(ActionType.DROP_ONE);
        removeAll(ActionType.DROP_STACK);
    }

    private void removeAll(ActionType type) {
        activeActions.removeIf(action -> {
            if (action.actionType == type) {
                action.deactivate();
                return true;
            }
            return false;
        });
    }

    public enum ActionType {
        SNEAK() {
            @Override
            public void tick(FakeEntityPlayer player) {
                if (!player.isSneaking())
                    player.setSneaking(true);
            }

            @Override
            public void deactivate(FakeEntityPlayer player) {
                player.setSneaking(false);
            }
        },
        SPRINT() {
            @Override
            public void tick(FakeEntityPlayer player) {
                if (!player.isSprinting())
                    player.setSprinting(true);
            }

            @Override
            public void deactivate(FakeEntityPlayer player) {
                player.setSprinting(false);
            }
        },
        USE() {
            @Override
            public void tick(FakeEntityPlayer player) {
                MovingObjectPosition rayTraceResult = player.getRayTrace(5, RayTrace.FluidCollisionOption.NONE);
                if (rayTraceResult instanceof MovingObjectPositionBlock) {
                    MovingObjectPositionBlock blockRayTraceResult = (MovingObjectPositionBlock) rayTraceResult;
                    WorldServer world = player.getWorldServer();
                    BlockPosition blockPosition = blockRayTraceResult.getBlockPosition();
                    EnumDirection direction = blockRayTraceResult.getDirection();
                    if (blockPosition.getY() < player.server.getMaxBuildHeight() - (direction == EnumDirection.UP ? 1 : 0)
                            && world.a/* canPlayerModifyAt */(player, blockPosition))
                        for (EnumHand hand : EnumHand.values())
                            if (player.playerInteractManager.a(player, world, player.getItemInHand(hand),
                                    hand, blockRayTraceResult) == EnumInteractionResult.SUCCESS) {
                                player.swingHand(hand);
                                return;
                            }
                } else if (rayTraceResult instanceof MovingObjectPositionEntity) {
                    MovingObjectPositionEntity entityRayTraceResult = (MovingObjectPositionEntity) rayTraceResult;
                    Entity hitEntity = entityRayTraceResult.getEntity();
                    Vec3D relativeHitPos = entityRayTraceResult.getPos()
                            .a/* subtract */(hitEntity.locX(), hitEntity.locY(), hitEntity.locZ());
                    for (EnumHand hand : EnumHand.values()) {
                        if (hitEntity.a(player, relativeHitPos, hand) == EnumInteractionResult.SUCCESS) return;
                        if (player.a(hitEntity, hand) == EnumInteractionResult.SUCCESS) return;
                    }
                }
                for (EnumHand hand : EnumHand.values())
                    if (player.playerInteractManager.a(player, player.getWorldServer(),
                            player.getItemInHand(hand), hand) == EnumInteractionResult.SUCCESS)
                        return;
            }

            @Override
            public void deactivate(FakeEntityPlayer player) {
                player.releaseActiveItem();
            }
        },
        DROP_ONE() {
            @Override
            public void tick(FakeEntityPlayer player) {
                player.dropItem(false);
            }

            @Override
            public void deactivate(FakeEntityPlayer player) {

            }
        },
        DROP_STACK() {
            @Override
            public void tick(FakeEntityPlayer player) {
                player.dropItem(true);
            }

            @Override
            public void deactivate(FakeEntityPlayer player) {

            }
        },
        DROP_ALL() {
            @Override
            public void tick(FakeEntityPlayer player) {
                dropList(player, player.inventory.armor);
                dropList(player, player.inventory.items);
                dropList(player, player.inventory.extraSlots);
            }

            private void dropList(FakeEntityPlayer player, List<ItemStack> list) {
                for(int i = 0; i < list.size(); ++i) {
                    ItemStack itemstack = list.get(i);
                    if (!itemstack.isEmpty()) {
                        player.a(itemstack, false, true);
                        list.set(i, ItemStack.b);
                    }
                }
            }

            @Override
            public void deactivate(FakeEntityPlayer player) {

            }
        };

        public abstract void tick(FakeEntityPlayer player);

        public abstract void deactivate(FakeEntityPlayer player);
    }

    public class Action {

        public final ActionType actionType;
        public final FakeEntityPlayer player;

        public final boolean isOnce;
        public final int interval;
        public final int repeats;

        private boolean isExecuted = false;
        private long ticks = -1L;

        public Action(ActionType actionType, FakeEntityPlayer player) {
            Preconditions.checkNotNull(actionType);
            Preconditions.checkNotNull(player);
            this.actionType = actionType;
            this.player = player;
            this.isOnce = true;
            this.interval = 1;
            this.repeats = 1;
        }

        public Action(ActionType actionType, FakeEntityPlayer player, int interval, int repeats) {
            Preconditions.checkNotNull(actionType);
            Preconditions.checkNotNull(player);
            Preconditions.checkArgument(interval > 0);
            Preconditions.checkArgument(repeats > 0);
            this.actionType = actionType;
            this.player = player;
            this.isOnce = false;
            this.interval = interval;
            this.repeats = repeats;
        }

        public void tick() {
            ticks++;
            if (isOnce && isExecuted) return;
            if (ticks % interval == 0) {
                isExecuted = true;
                for (int i = 0; i < repeats; i++) actionType.tick(player);
            }
        }

        public void deactivate() {
            actionType.deactivate(player);
        }

        public boolean isCompleted() {
            return isOnce && isExecuted;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Action action = (Action) o;
            return isOnce == action.isOnce &&
                    interval == action.interval &&
                    repeats == action.repeats &&
                    actionType == action.actionType &&
                    player.equals(action.player);
        }

        @Override
        public int hashCode() {
            return Objects.hash(actionType, player, isOnce, interval, repeats);
        }

        @Override
        public String toString() {
            return "Action{" +
                    "actionType=" + actionType +
                    ", player=" + player +
                    ", isOnce=" + isOnce +
                    ", interval=" + interval +
                    ", repeats=" + repeats +
                    ", isExecuted=" + isExecuted +
                    ", ticks=" + ticks +
                    '}';
        }
    }

}
