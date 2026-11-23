package com.electrifiedded.archaiclib.ai.path;

/** How a mob gets from one path node (feet cell) to the next. */
public enum Move {
    START,
    /** one block N/S/E/W on the same level (digging the way, bridging a gap) */
    TRAVERSE,
    /** one block diagonally, only when both side cells are free (no corner clipping) */
    DIAGONAL,
    /** jump one block up onto the next cell (digging headroom, placing the step) */
    ASCEND,
    /** walk one block down */
    DESCEND,
    /** walk off an edge and fall two or more blocks */
    FALL,
    /** dig the floor and drop into the hole */
    DOWNWARD,
    /** jump and put a block under itself */
    PILLAR,
    CLIMB_UP,
    CLIMB_DOWN,
    SWIM_UP,
    SWIM_DOWN
}
