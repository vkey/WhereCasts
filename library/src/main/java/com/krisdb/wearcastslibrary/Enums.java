package com.krisdb.wearcastslibrary;


public class Enums {
    public enum SkipDirection
    {
        PREVIOUS, NEXT
    }

    public enum SortOrder
    {
        NAMEASC (1),
        NAMEDESC (2),
        DATEASC (3),
        DATEDESC (4),
        DATEDOWNLOADED_DESC (5),
        DATEDOWNLOADED_ASC (6),
        DATEADDED_DESC (7),
        DATEADDED_ASC (8),
        PROGRESS (9),
        NEWEPISODES (10),
        LATESTEPISODES (11)
        ;

        private final int sortOrder;

        SortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
        }

        public int getSorderOrderCode() {
            return this.sortOrder;
        }
    }

    public enum HomeScreen
    {
        DEFAULT (0),
        LOCAL (1),
        INPROGRESS (2),
        DOWNLOADS (3),
        UPNEXT (4);

        private final int homescreen;

        HomeScreen(int homeScreen) {
            this.homescreen = homeScreen;
        }

        public int getSorderOrderCode() {
            return this.homescreen;
        }
    }

    public enum AutoDelete
    {
        NEVER (0),
        PLAYED (1),
        TWENTYFOURHOURS (2),
        TWODAYS (3),
        ONEEEK (4);

        private final int autodeleteid;

        AutoDelete(int id) {
            this.autodeleteid = id;
        }

        public int getAutoDeleteID() {
            return this.autodeleteid;
        }
    }

    public enum ThemeOptions
    {
        DEFAULT (0),
        DARK (1),
        LIGHT (2),
        AMOLED(3),
        DYNAMIC (4);

        private final int theme;

        ThemeOptions(int theme) {
            this.theme = theme;
        }

        public int getThemeId() {
            return this.theme;
        }
    }

    public enum PreferenceThemeOptions
    {
        DEFAULT (0),
        DARK (1),
        LIGHT (2),
        AMOLED(3),
        DYNAMIC (4);

        private final int theme;

        PreferenceThemeOptions(int theme) {
            this.theme = theme;
        }

        public int getThemeId() {
            return this.theme;
        }
    }

    public enum FontSize
    {
        SMALL (0),
        NORMAL (1),
        LARGE (2);

        private final int size;

        FontSize(int size) {
            this.size = size;
        }

        public int getFontSizeID() {
            return this.size;
        }
    }
}
