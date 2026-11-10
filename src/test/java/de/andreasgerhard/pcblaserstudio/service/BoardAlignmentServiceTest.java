package de.andreasgerhard.pcblaserstudio.service;

import de.andreasgerhard.pcblaserstudio.model.BoardSettings;
import de.andreasgerhard.pcblaserstudio.model.FlipAxis;
import de.andreasgerhard.pcblaserstudio.model.LayerSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardAlignmentServiceTest {

    private static final double EPS = 1e-9;

    /**
     * Board 100 x 80 mm, lower-left at (10, 20) on the bed, margin 5 mm,
     * SVG canvas 50 x 40 mm.
     */
    private static BoardSettings board() {
        BoardSettings b = new BoardSettings();
        b.setWidthMm(100.0);
        b.setHeightMm(80.0);
        b.setMarginMm(5.0);
        b.setPositionXMm(10.0);
        b.setPositionYMm(20.0);
        return b;
    }

    @Test
    void frontCanvasLowerLeftIsBoardPositionPlusMargin() {
        BoardAlignmentService.Offset o =
                BoardAlignmentService.computeBaseOffset(board(), LayerSide.FRONT, 50.0, 40.0);

        assertEquals(15.0, o.xMm(), EPS);
        assertEquals(25.0, o.yMm(), EPS);
    }

    @Test
    void backVerticalFlipMirrorsXOnly() {
        BoardAlignmentService.Offset o =
                BoardAlignmentService.computeBaseOffset(board(), LayerSide.BACK, 50.0, 40.0);

        // Default flip axis is VERTICAL: 10 + 100 - 5 - 50 = 55.
        assertEquals(55.0, o.xMm(), EPS);
        assertEquals(25.0, o.yMm(), EPS);
    }

    @Test
    void backHorizontalFlipMirrorsYOnly() {
        BoardSettings b = board();
        b.setFlipAxis(FlipAxis.HORIZONTAL);

        BoardAlignmentService.Offset o =
                BoardAlignmentService.computeBaseOffset(b, LayerSide.BACK, 50.0, 40.0);

        assertEquals(15.0, o.xMm(), EPS);
        // 20 + 80 - 5 - 40 = 55.
        assertEquals(55.0, o.yMm(), EPS);
    }

    @Test
    void previewTopLeftIsBoardLocalAndBottomAnchored() {
        BoardAlignmentService.Offset o =
                BoardAlignmentService.computeContentTopLeftInBoard(
                        board(), LayerSide.FRONT, 50.0, 40.0);

        // Bottom margin 5 -> top at 80 - 5 - 40 = 35.
        assertEquals(5.0, o.xMm(), EPS);
        assertEquals(35.0, o.yMm(), EPS);
    }

    @Test
    void previewTopLeftForBackVerticalMirrorsXOnly() {
        BoardAlignmentService.Offset o =
                BoardAlignmentService.computeContentTopLeftInBoard(
                        board(), LayerSide.BACK, 50.0, 40.0);

        assertEquals(45.0, o.xMm(), EPS);
        assertEquals(35.0, o.yMm(), EPS);
    }

    @Test
    void previewTopLeftForBackHorizontalMirrorsToTop() {
        BoardSettings b = board();
        b.setFlipAxis(FlipAxis.HORIZONTAL);

        BoardAlignmentService.Offset o =
                BoardAlignmentService.computeContentTopLeftInBoard(
                        b, LayerSide.BACK, 50.0, 40.0);

        assertEquals(5.0, o.xMm(), EPS);
        assertEquals(5.0, o.yMm(), EPS);
    }

    @Test
    void svgBorderRectMatchesFrontBorderPass() {
        BoardAlignmentService.Rect r =
                BoardAlignmentService.computeSvgBorderRect(
                        board(), LayerSide.FRONT, 50.0, 40.0);

        assertEquals(15.0, r.xMm(), EPS);
        assertEquals(25.0, r.yMm(), EPS);
        assertEquals(50.0, r.widthMm(), EPS);
        assertEquals(40.0, r.heightMm(), EPS);
    }

    @Test
    void svgBorderRectIsMirroredForBackVerticalFlip() {
        BoardAlignmentService.Rect r =
                BoardAlignmentService.computeSvgBorderRect(
                        board(), LayerSide.BACK, 50.0, 40.0);

        assertEquals(55.0, r.xMm(), EPS);
        assertEquals(25.0, r.yMm(), EPS);
        assertEquals(50.0, r.widthMm(), EPS);
        assertEquals(40.0, r.heightMm(), EPS);
    }

    @Test
    void svgBorderRectIsMirroredForBackHorizontalFlip() {
        BoardSettings b = board();
        b.setFlipAxis(FlipAxis.HORIZONTAL);

        BoardAlignmentService.Rect r =
                BoardAlignmentService.computeSvgBorderRect(
                        b, LayerSide.BACK, 50.0, 40.0);

        assertEquals(15.0, r.xMm(), EPS);
        assertEquals(55.0, r.yMm(), EPS);
        assertEquals(50.0, r.widthMm(), EPS);
        assertEquals(40.0, r.heightMm(), EPS);
    }
}
