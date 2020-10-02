package com.ishland.bukkit.carpetplugin.lib.fakeplayer.action;

import com.google.common.base.Preconditions;
import com.ishland.bukkit.carpetplugin.lib.fakeplayer.base.FakeEntityPlayer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class FakeEntityPlayerActionPack {

    private final FakeEntityPlayer fakeEntityPlayer;
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
        private long ticks = 0L;

        public Action(ActionType actionType, FakeEntityPlayer player, int delay) {
            Preconditions.checkNotNull(actionType);
            Preconditions.checkNotNull(player);
            Preconditions.checkArgument(delay > 0);
            this.actionType = actionType;
            this.player = player;
            this.isOnce = true;
            this.interval = delay;
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
    }

}
