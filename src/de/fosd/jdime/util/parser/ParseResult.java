/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2018 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.util.parser;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A <code>List</code> of <code>Content</code> instances resulting from a run of {@link Parser#parse(String)}.
 * In addition to the pieces of content the lines of code, the number of lines of code in conflicts and the number
 * of conflicts are stored. The {@link #toString()} and {@link #toString(String, String)} methods produce the
 * concatenation of the <code>String</code> representations of the <code>Content</code> pieces.
 */
public class ParseResult extends ArrayList<Content> {

    private static final long serialVersionUID = 1L;

    /**
     * The last {@link #hashCode()} for which the statistics below were calculated. This is used for lazy
     * (re)initialization of these attributes.
     */
    private Optional<Integer> statsHash;

    private int conflicts;

    private int linesOfCode;
    private int conflictingLinesOfCode;

    private int chars;
    private int conflictingChars;

    private int tokens;
    private int conflictingTokens;

    private String leftLabel = Content.Conflict.DEFAULT_LABEL;
    private String rightLabel = Content.Conflict.DEFAULT_LABEL;

    public ParseResult() {
        this.statsHash = Optional.empty();
    }

    /**
     * Returns the lines of code.
     *
     * @return the lines of code
     */
    public int getLinesOfCode() {
        return linesOfCode;
    }

    /**
     * Sets the lines of code to the new value.
     *
     * @param linesOfCode
     *         the new lines of code
     */
    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    /**
     * Returns the number of conflicts.
     *
     * @return the number of conflicts
     */
    public int getConflicts() {
        return conflicts;
    }

    /**
     * Sets the number of conflicts to the new value.
     *
     * @param conflicts
     *         the new number of conflicts
     */
    public void setConflicts(int conflicts) {
        this.conflicts = conflicts;
    }

    /**
     * Returns the number of non-whitespace characters.
     *
     * @return the number of characters
     */
    public int getChars() {
        return chars;
    }

    /**
     * Sets the number of non-whitespace characters.
     *
     * @param chars the new number of chars
     */
    public void setChars(int chars) {
        this.chars = chars;
    }

    /**
     * Returns the number of non-whitespace characters in conflict.
     *
     * @return the number of non-whitespace characters in conflict
     */
    public int getConflictingChars() {
        return conflictingChars;
    }

    /**
     * Sets the number of non-whitespace characters in conflict to the new value.
     *
     * @param conflictingChars the new number of conflicting chars
     */
    public void setConflictingChars(int conflictingChars) {
        this.conflictingChars = conflictingChars;
    }

    /**
     * Returns the number of tokens.
     *
     * @return the number of tokens
     */
    public int getTokens() {
        return tokens;
    }

    /**
     * Sets the number of tokens.
     *
     * @param tokens the new number of tokens
     */
    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    /**
     * Returns the number of tokens in conflict.
     *
     * @return the number of tokens in conflict
     */
    public int getConflictingTokens() {
        return conflictingTokens;
    }

    /**
     * Sets the number of tokens in conflict.
     *
     * @param conflictingTokens the new number of tokens in conflict
     */
    public void setConflictingTokens(int conflictingTokens) {
        this.conflictingTokens = conflictingTokens;
    }

    /**
     * Returns the conflicting lines of code.
     *
     * @return the conflicting lines of code
     */
    public int getConflictingLinesOfCode() {
        return conflictingLinesOfCode;
    }

    /**
     * Sets the conflicting lines of code to the new value.
     *
     * @param conflictingLinesOfCode
     *         the new conflicting lines of code
     */
    public void setConflictingLinesOfCode(int conflictingLinesOfCode) {
        this.conflictingLinesOfCode = conflictingLinesOfCode;
    }

    /**
     * Sets the label for the left side of any {@link Content.Conflict} in this {@link ParseResult}.
     *
     * @param leftLabel the label for the left side
     */
    public void setLeftLabel(String leftLabel) {
        this.leftLabel = leftLabel;
    }

    /**
     * Sets the label for the left side of any {@link Content.Conflict} in this {@link ParseResult}.
     *
     * @param rightLabel the label for the right side
     */
    public void setRightLabel(String rightLabel) {
        this.rightLabel = rightLabel;
    }

    /**
     * Adds a merged line (one that is not in a conflict) to the <code>ParseResult</code>.
     *
     * @param line
     *         the line to add
     */
    public void addMergedLine(String line) {
        Content.Merged lines;

        if (isEmpty()) {
            lines = new Content.Merged();
            add(lines);
        } else {
            Content content = get(size() - 1);

            if (!content.isConflict()) {
                lines = (Content.Merged) content;
            } else {
                lines = new Content.Merged();
                add(lines);
            }
        }

        lines.add(line);
    }

    /**
     * Adds a line that is part of a conflict to the <code>ParseResult</code>.
     *
     * @param line
     *         the line to add
     * @param left
     *         true iff the line is part of the left side of the conflict (otherwise it is part of the right side)
     */
    public void addConflictingLine(String line, boolean left) {
        Content.Conflict conflict;

        if (isEmpty()) {
            conflict = new Content.Conflict();
            add(conflict);
        } else {
            Content content = get(size() - 1);

            if (content.isConflict()) {
                conflict = (Content.Conflict) content;
            } else {
                conflict = new Content.Conflict();
                add(conflict);
            }
        }

        if (left) {
            conflict.addLeft(line);
        } else {
            conflict.addRight(line);
        }
    }

    /**
     * Returns the last {@link Content} added to this {@link ParseResult}.
     *
     * @return the last {@link Content} or {@code null}
     */
    Content getLast() {
        return isEmpty() ? null : get(size() - 1);
    }

    /**
     * {@inheritDoc}
     * <br><br>
     * The {@link Content.Conflict Conflicts} in this {@link ParseResult} will be labeled using the labels set via
     * {@link #setLeftLabel(String)} and {@link #setRightLabel(String)}.
     *
     * @see Content#toString(String, String)
     */
    @Override
    public String toString() {
        return toString(leftLabel, rightLabel);
    }

    /**
     * Returns a {@link String} representation of this {@link ParseResult}. The given labels will be applied to the
     * sides of any {@link Content.Conflict} elements in this {@link ParseResult}.
     *
     * @param leftLabel  the label for the left side of any conflict in the {@link ParseResult}
     * @param rightLabel the label for the right side of any conflict in the {@link ParseResult}
     * @return a {@link String} representing this {@link ParseResult}
     */
    public String toString(String leftLabel, String rightLabel) {
        return stream().map(c -> c.toString(leftLabel, rightLabel)).collect(Collectors.joining(System.lineSeparator()));
    }
}
