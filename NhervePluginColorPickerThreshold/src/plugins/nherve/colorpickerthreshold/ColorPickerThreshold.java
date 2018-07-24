/*
 * Copyright 2010, 2011 Institut Pasteur.
 * Copyright 2012, 2013 Nicolas Hervé. 
 * 
 * This file is part of Color Picker Threshold, which is an ICY plugin.
 * 
 * Color Picker Threshold is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Color Picker Threshold is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Color Picker Threshold. If not, see <http://www.gnu.org/licenses/>.
 */
package plugins.nherve.colorpickerthreshold;

import icy.canvas.IcyCanvas;
import icy.gui.component.ComponentUtil;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.painter.Painter;
import icy.roi.ROI2DArea;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.type.TypeUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plugins.nherve.toolbox.Algorithm;
import plugins.nherve.toolbox.NherveToolbox;
import plugins.nherve.toolbox.image.BinaryIcyBufferedImage;
import plugins.nherve.toolbox.image.feature.ColorDistance;
import plugins.nherve.toolbox.image.feature.L1ColorDistance;
import plugins.nherve.toolbox.image.feature.L2ColorDistance;
import plugins.nherve.toolbox.image.feature.SegmentableIcyBufferedImage;
import plugins.nherve.toolbox.image.feature.descriptor.ColorPixel;
import plugins.nherve.toolbox.image.feature.learning.SVMClassifier;
import plugins.nherve.toolbox.image.feature.region.IcyPixel;
import plugins.nherve.toolbox.image.feature.signature.DefaultVectorSignature;
import plugins.nherve.toolbox.image.feature.signature.DenseVectorSignature;
import plugins.nherve.toolbox.image.feature.signature.SignatureException;
import plugins.nherve.toolbox.image.feature.signature.VectorSignature;
import plugins.nherve.toolbox.image.mask.Mask;
import plugins.nherve.toolbox.image.mask.MaskException;
import plugins.nherve.toolbox.image.toolboxes.ColorSpaceTools;
import plugins.nherve.toolbox.libsvm.svm;
import plugins.nherve.toolbox.libsvm.svm_parameter;
import plugins.nherve.toolbox.plugin.PainterManagerSingletonPlugin;

/**
 * The Class ColorPickerThreshold.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class ColorPickerThreshold extends PainterManagerSingletonPlugin<ColorPickerThreshold.ColorPickerThresholdPainter> implements ActionListener, ChangeListener, ItemListener {

	class ColorBox extends JPanel implements ActionListener {

		/**
		 * The color selector manages and displays the colors picked by the
		 * user.
		 * 
		 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
		 */
		private class ColorSelector extends JPanel implements MouseListener {
			/** The Constant serialVersionUID. */
			private static final long serialVersionUID = 5232546251526771415L;

			private ColorBox box;

			/**
			 * Instantiates a new color box.
			 */
			public ColorSelector(ColorBox box) {
				super();
				this.box = box;
				addMouseListener(this);
				setOpaque(true);
			}

			public Dimension getPreferredSize() {
				return COL_DIM;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent
			 * )
			 */
			@Override
			public void mouseClicked(MouseEvent me) {
				if (me.getButton() == MouseEvent.BUTTON1) {
					int row = (int) Math.floor((double) me.getY() / (double) COL_FULL_BLOCK_SIZE);
					int col = (int) Math.floor((double) me.getX() / (double) COL_FULL_BLOCK_SIZE);
					box.removeColor(row, col);
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent
			 * )
			 */
			@Override
			public void mouseEntered(MouseEvent e) {
				// Nothing to do here
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent
			 * )
			 */
			@Override
			public void mouseExited(MouseEvent e) {
				// Nothing to do here
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent
			 * )
			 */
			@Override
			public void mousePressed(MouseEvent e) {
				// Nothing to do here
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent
			 * )
			 */
			@Override
			public void mouseReleased(MouseEvent e) {
				// Nothing to do here
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
			 */
			public void paintComponent(Graphics g) {
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());

				for (int row = 0; row < COL_GRID_HEIGHT; row++) {
					int y = row * (COL_FULL_BLOCK_SIZE + COL_BLOCK_SPACER);
					for (int column = 0; column < COL_GRID_WIDTH; column++) {
						int x = column * (COL_FULL_BLOCK_SIZE + COL_BLOCK_SPACER);
						g.setColor(box.getColor(row, column));
						g.fillRect(x, y, COL_BLOCK_SIZE, COL_BLOCK_SIZE);
						g.setColor(Color.black);
						g.drawRect(x, y, COL_BLOCK_SIZE, COL_BLOCK_SIZE);
					}
				}
			}
		}

		private static final long serialVersionUID = -2779071071132708790L;

		/** The colors. */
		private Color[] colors;
		/** The nb colors. */
		private int nbColors;
		/** The threshold. */
		private int threshold;

		/** The listeners. */
		private ArrayList<ColorBoxListener> listeners;

		private ColorSelector selector;

		private JButton btInitColors;

		public ColorBox(String label) {
			super();

			colors = new Color[COL_GRID_MAXCOLORS];
			listeners = new ArrayList<ColorBoxListener>();

			selector = new ColorSelector(this);
			btInitColors = new JButton("Clear");
			btInitColors.setEnabled(false);
			btInitColors.addActionListener(this);

			JPanel p1 = GuiUtil.createLineBoxPanel(new JLabel(label), Box.createHorizontalGlue(), btInitColors);
			JPanel p2 = GuiUtil.createPageBoxPanel(selector, Box.createVerticalGlue(), p1);
			add(p2);

			initColors();
		}

		/**
		 * Adds the color.
		 * 
		 * @param c
		 *            the c
		 */
		public void addColor(int[] c) {
			if (nbColors < COL_GRID_MAXCOLORS) {
				colors[nbColors] = new Color(c[0], c[1], c[2]);
				nbColors++;
				btFilter.setEnabled(true);
				btKeepMask.setEnabled(true);
				btAsROI.setEnabled(true);
				btInitColors.setEnabled(true);
				repaint();
				fireFilterParametersChangeEvent();
			}
		}

		public Color getColor(int row, int col) {
			return colors[row * COL_GRID_WIDTH + col];
		}

		/**
		 * Fire cancel filter event.
		 */
		public void fireCancelFilterEvent() {
			for (ColorBoxListener l : listeners) {
				l.cancelFilter();
			}
		}

		/**
		 * Fire display parameters change event.
		 */
		public void fireDisplayParametersChangeEvent() {
			for (ColorBoxListener l : listeners) {
				l.displayParametersHaveChanged();
			}
		}

		/**
		 * Fire filter parameters change event.
		 */
		public void fireFilterParametersChangeEvent() {
			for (ColorBoxListener l : listeners) {
				l.filterParametersHaveChanged();
			}
		}

		/**
		 * Gets the average color.
		 * 
		 * @return the average color
		 */
		public Color getAverageColor() {
			int r = 0;
			int g = 0;
			int b = 0;
			for (int k = 0; k < nbColors; k++) {
				r += colors[k].getRed();
				g += colors[k].getGreen();
				b += colors[k].getBlue();
			}
			r /= nbColors;
			g /= nbColors;
			b /= nbColors;

			return new Color(r, g, b);
		}

		/**
		 * Gets the threshold.
		 * 
		 * @return the threshold
		 */
		public int getThreshold() {
			return threshold;
		}

		/**
		 * Inits the colors.
		 */
		public void initColors() {
			nbColors = 0;
			for (int i = 0; i < COL_GRID_MAXCOLORS; i++) {
				colors[i] = getBackground();
			}
			repaint();
			fireFilterParametersChangeEvent();
		}

		/**
		 * Register.
		 * 
		 * @param l
		 *            the l
		 */
		public void register(ColorBoxListener l) {
			listeners.add(l);
		}

		/**
		 * Removes the.
		 * 
		 * @param l
		 *            the l
		 */
		public void remove(ColorBoxListener l) {
			listeners.remove(l);
		}

		public void removeColor(int row, int col) {
			if ((row < COL_GRID_HEIGHT) && (col < COL_GRID_WIDTH)) {
				int idx = row * COL_GRID_WIDTH + col;
				if (idx < nbColors) {
					for (int c = idx; (c < nbColors) && (c < COL_GRID_MAXCOLORS - 1); c++) {
						colors[c] = colors[c + 1];
					}
					colors[nbColors - 1] = getBackground();
					nbColors--;
					if (nbColors == 0) {
						btInitColors.setEnabled(false);
					}
					repaint();
					fireFilterParametersChangeEvent();
				}
			}
		}

		/**
		 * Sets the threshold.
		 * 
		 * @param threshold
		 *            the new threshold
		 */
		public void setThreshold(int threshold) {
			this.threshold = threshold;
			fireFilterParametersChangeEvent();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object o = e.getSource();

			if (o == null) {
				return;
			}

			if (o instanceof JButton) {
				JButton b = (JButton) e.getSource();
				if (b == null) {
					return;
				}

				if (b == btInitColors) {
					initColors();
					btInitColors.setEnabled(false);
				}
			}
		}

	}

	/**
	 * The Interface ColorBoxListener.
	 * 
	 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
	 */
	interface ColorBoxListener {

		/**
		 * Cancel filter.
		 */
		void cancelFilter();

		/**
		 * Display parameters have changed.
		 */
		void displayParametersHaveChanged();

		/**
		 * Filter parameters have changed.
		 */
		void filterParametersHaveChanged();
	}

	/**
	 * This Painter is needed to access the colors picked in the image.
	 * 
	 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
	 */
	class ColorPickerThresholdPainter implements Painter, ColorBoxListener {

		/** The mask. */
		private Mask mask;

		/** The sequence. */
		private Sequence sequence;

		/**
		 * Instantiates a new color picker threshold painter.
		 */
		public ColorPickerThresholdPainter() {
			super();

			setMask(null);
			setSequence(null);
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * plugins.nherve.colorpickerthreshold.ColorPickerThreshold.ColorBoxListener
		 * #cancelFilter()
		 */
		@Override
		public void cancelFilter() {
			setMask(null);
			displayParametersHaveChanged();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * plugins.nherve.colorpickerthreshold.ColorPickerThreshold.ColorBoxListener
		 * #displayParametersHaveChanged()
		 */
		@Override
		public void displayParametersHaveChanged() {
			getSequence().painterChanged(null);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * plugins.nherve.colorpickerthreshold.ColorPickerThreshold.ColorBoxListener
		 * #filterParametersHaveChanged()
		 */
		@Override
		public void filterParametersHaveChanged() {
			if (cbAuto.isSelected()) {
				try {
					doFilter(this);
				} catch (SignatureException e1) {
					Algorithm.err(e1);
				}
			}
		}

		/**
		 * Gets the clicked color.
		 * 
		 * @param pt
		 *            the pt
		 * @return the clicked color
		 * @throws SignatureException
		 *             the signature exception
		 */
		private int[] getClickedColor(Point pt) throws SignatureException {
			IcyBufferedImage image = getCurrentSequence().getFirstImage();
			int[] c = null;
			if (image.isInside(pt)) {
				int x = (int) pt.getX();
				int y = (int) pt.getY();
				c = ColorSpaceTools.getColorComponentsI_0_255(image, ColorSpaceTools.RGB, x, y);
			}

			return c;
		}

		/**
		 * Gets the mask.
		 * 
		 * @return the mask
		 */
		public Mask getMask() {
			return mask;
		}

		/**
		 * Gets the sequence.
		 * 
		 * @return the sequence
		 */
		public Sequence getSequence() {
			return sequence;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#keyPressed(java.awt.event.KeyEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#keyReleased(java.awt.event.KeyEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mouseClick(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
			int comp = tabbedPane.getSelectedIndex();
			try {
				if (comp == 0) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						int[] c = getClickedColor(TypeUtil.toPoint(imagePoint));
						if (c != null) {
							m1ColorBox.addColor(c);
						}
					}
				} else if (comp == 1) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						int[] c = getClickedColor(TypeUtil.toPoint(imagePoint));
						if (c != null) {
							m2PosColorBox.addColor(c);
						}
					} else if ((e.getButton() == MouseEvent.BUTTON2) || (e.getButton() == MouseEvent.BUTTON3)) {
						int[] c = getClickedColor(TypeUtil.toPoint(imagePoint));
						if (c != null) {
							m2NegColorBox.addColor(c);
						}
					}
				}

			} catch (SignatureException ex) {
				Algorithm.err(ex);
			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mouseDrag(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mouseDrag(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mouseMove(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mouseMove(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mousePressed(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#mouseReleased(java.awt.event.MouseEvent,
		 * java.awt.geom.Point2D, icy.canvas.IcyCanvas)
		 */
		@Override
		public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see icy.painter.Painter#paint(java.awt.Graphics2D,
		 * icy.sequence.Sequence, icy.canvas.IcyCanvas)
		 */
		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			if ((getMask() != null) && (cbShow.isSelected())) {
				getMask().paint(g);
			}
		}

		/**
		 * Sets the mask.
		 * 
		 * @param mask
		 *            the new mask
		 */
		public void setMask(Mask mask) {
			this.mask = mask;
		}

		/**
		 * Sets the sequence.
		 * 
		 * @param sequence
		 *            the new sequence
		 */
		public void setSequence(Sequence sequence) {
			this.sequence = sequence;
		}

	}

	private final static int COL_BLOCK_BORDER = 1;

	private final static int COL_BLOCK_SPACER = 1;

	private final static int COL_BLOCK_SIZE = 25;

	/** The Constant COL_GRID_HEIGHT. */
	private final static int COL_GRID_HEIGHT = 3;

	/** The Constant COL_GRID_WIDTH. */
	private final static int COL_GRID_WIDTH = 8;

	private final static int COL_FULL_BLOCK_SIZE = COL_BLOCK_SIZE + 2 * COL_BLOCK_BORDER;
	private final static Dimension COL_DIM = new Dimension(COL_GRID_WIDTH * (COL_FULL_BLOCK_SIZE + COL_BLOCK_SPACER) - 1, COL_GRID_HEIGHT * (COL_FULL_BLOCK_SIZE + COL_BLOCK_SPACER) - 1);

	/** The Constant COL_GRID_MAXCOLORS. */
	private final static int COL_GRID_MAXCOLORS = COL_GRID_HEIGHT * COL_GRID_WIDTH;

	/** The Constant METHOD_1. */
	private final static String METHOD_1 = "KNN";

	/** The Constant METHOD_2. */
	private final static String METHOD_2 = "SVM";

	/** The bt cancel filter. */
	private JButton btCancelFilter;

	/** The bt filter. */
	private JButton btFilter;

	/** The bt keep mask. */
	private JButton btKeepMask;

	/** The bt as roi. */
	private JButton btAsROI;

	/** The bt minus. */
	private JButton btMinus;

	/** The bt plus. */
	private JButton btPlus;

	/** The col default dist. */
	private int colDefaultDist;

	/** The col max dist. */
	private int colMaxDist;

	/** The m1 color box. */
	private ColorBox m1ColorBox;

	/** The m2 pos color box. */
	private ColorBox m2PosColorBox;

	/** The m2 neg color box. */
	private ColorBox m2NegColorBox;

	/** The lb current. */
	private JLabel lbCurrent;

	/** The rb rgb. */
	private JRadioButton rbRGB;

	/** The rb hsv. */
	private JRadioButton rbHSV;

	/** The rb h1 h2 h3. */
	private JRadioButton rbH1H2H3;

	/** The rb l1. */
	private JRadioButton rbL1;

	/** The rb l2. */
	private JRadioButton rbL2;

	/** The rb kernel lin. */
	private JRadioButton rbKernelLin;

	/** The rb kernel rbf. */
	private JRadioButton rbKernelRBF;

	/** The rb kernel tri. */
	private JRadioButton rbKernelTri;

	/** The sl c. */
	private JSlider slC;

	/** The sl gamma. */
	private JSlider slGamma;

	/** The val c. */
	private JLabel valC;

	/** The val gamma. */
	private JLabel valGamma;

	/** The distance. */
	private ColorDistance distance;

	/** The choosen cs. */
	private int choosenCS;

	/** The sl dist threshold. */
	private JSlider slDistThreshold;

	/** The sl maj tick. */
	private int slMajTick;

	/** The sl min tick. */
	private int slMinTick;

	/** The cb show. */
	private JCheckBox cbShow;

	/** The cb auto. */
	private JCheckBox cbAuto;

	/** The tabbed pane. */
	private JTabbedPane tabbedPane;

	/**
	 * Instantiates a new color picker threshold.
	 */
	public ColorPickerThreshold() {
		super();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == null) {
			return;
		}

		if (o instanceof JButton) {
			JButton b = (JButton) e.getSource();
			if (b == null) {
				return;
			}

			if (b == btFilter) {
				if (hasCurrentSequence()) {
					try {
						doFilter(getCurrentSequencePainter());
					} catch (SignatureException e1) {
						Algorithm.err(e1);
					}
				}

				return;
			}

			if (b == btCancelFilter) {
				if (cbAuto.isSelected()) {
					m1ColorBox.fireCancelFilterEvent();
				} else if (hasCurrentSequence()) {
					getCurrentSequencePainter().setMask(null);
					getCurrentSequence().painterChanged(null);
				}
				return;
			}

			if (b == btPlus) {
				if (slDistThreshold.getValue() < slDistThreshold.getMaximum()) {
					slDistThreshold.setValue(slDistThreshold.getValue() + 1);
				}
				return;
			}

			if (b == btMinus) {
				if (slDistThreshold.getValue() > slDistThreshold.getMinimum()) {
					slDistThreshold.setValue(slDistThreshold.getValue() - 1);
				}
				return;
			}

			if (b == btKeepMask) {
				if (hasCurrentSequence()) {
					Sequence currentSequence = getCurrentSequence();
					try {
						Mask m = doFilter(getCurrentSequencePainter());
						if (m != null) {
							SwimmingObject result = new SwimmingObject(m);
							Icy.getMainInterface().getSwimmingPool().add(result);
						}
					} catch (SignatureException e1) {
						Algorithm.err(e1);
					}
					currentSequence.dataChanged();
				}
				return;
			}

			if (b == btAsROI) {
				if (hasCurrentSequence()) {
					Sequence currentSequence = getCurrentSequence();
					try {
						Mask m = doFilter(getCurrentSequencePainter());
						if (m != null) {
							ROI2DArea a = m.asROI2DArea(currentSequence);
							a.setName("From " + getName());
						}
					} catch (SignatureException e1) {
						Algorithm.err(e1);
					}
					currentSequence.dataChanged();
				}
				return;
			}
		}

		if (o instanceof JRadioButton) {
			
			JRadioButton b = (JRadioButton) e.getSource();
			if (b == rbRGB) {
				choosenCS = ColorSpaceTools.RGB;
			}
			if (b == rbHSV) {
				choosenCS = ColorSpaceTools.RGB_TO_HSV;
			}
			if (b == rbH1H2H3) {
				choosenCS = ColorSpaceTools.RGB_TO_H1H2H3;
			}
			if ((b == rbL1) || (b == rbL2)) {
				double currentPct = (double) slDistThreshold.getValue() / (double) slDistThreshold.getMaximum();

				if (b == rbL1) {
					setDistance(new L1ColorDistance());
				}
				if (b == rbL2) {
					setDistance(new L2ColorDistance());
				}

				slDistThreshold.setMaximum(colMaxDist);
				int currentValue = (int) (currentPct * slDistThreshold.getMaximum());
				slDistThreshold.setValue(currentValue);
				m1ColorBox.setThreshold(currentValue);
				slDistThreshold.setMajorTickSpacing(slMajTick);
				slDistThreshold.setMinorTickSpacing(slMinTick);
			}
			if (hasCurrentSequence() && cbAuto.isSelected()) {
				try {
					doFilter(getCurrentSequencePainter());
				} catch (SignatureException e1) {
					Algorithm.err(e1);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.PainterFactory#createNewPainter()
	 */
	@Override
	public ColorPickerThresholdPainter createNewPainter() {
		ColorPickerThresholdPainter painter = new ColorPickerThresholdPainter();
		Sequence currentSequence = getCurrentSequence();
		painter.setSequence(currentSequence);
		m1ColorBox.register(painter);
		m2PosColorBox.register(painter);
		m2NegColorBox.register(painter);
		try {
			doFilter(painter);
		} catch (SignatureException e1) {
			Algorithm.err(e1);
		}
		return painter;
	}

	/**
	 * Do filter.
	 * 
	 * @param painter
	 *            the painter
	 * @return the mask
	 * @throws SignatureException
	 *             the signature exception
	 */
	private Mask doFilter(ColorPickerThresholdPainter painter) throws SignatureException {
		Sequence currentSequence = painter.getSequence();
		IcyBufferedImage currentImage = currentSequence.getFirstImage();
		Mask m = null;
		int comp = tabbedPane.getSelectedIndex();
		try {
			if (comp == 0) {
				m = filter1(m1ColorBox, currentImage);
				m.setLabel(getName() + " " + METHOD_1);
			} else if (comp == 1) {
				m = filter2(m2PosColorBox, m2NegColorBox, currentImage);
				m.setLabel(getName() + " " + METHOD_2);
			}
		} catch (MaskException e) {
			// ignore
		}
		painter.setMask(m);
		currentSequence.painterChanged(null);
		return m;
	}

	/**
	 * Filter1.
	 * 
	 * @param box
	 *            the box
	 * @param image
	 *            the image
	 * @return the mask
	 * @throws MaskException
	 *             the mask exception
	 * @throws SignatureException
	 *             the signature exception
	 */
	private Mask filter1(ColorBox box, IcyBufferedImage image) throws MaskException, SignatureException {
		if (box.nbColors == 0) {
			throw new MaskException("No color selected, filtering aborted");
		}

		// CPUMonitor moni = new CPUMonitor();
		// moni.start();

		// System.out.println("starting filtering");
		// Raster raster = image.getRaster();
		Mask m = new Mask(image.getWidth(), image.getHeight(), false);
		BinaryIcyBufferedImage bin = m.getBinaryData();

		ArrayList<double[]> csColors = new ArrayList<double[]>();
		for (int k = 0; k < box.nbColors; k++) {
			csColors.add(ColorSpaceTools.getColorComponentsD_0_255(choosenCS, box.colors[k].getRed(), box.colors[k].getGreen(), box.colors[k].getBlue()));
		}

		byte[] raw = bin.getRawData();
		int idx = 0;
		for (int j = 0; j < image.getHeight(); j++) {
			for (int i = 0; i < image.getWidth(); i++) {
				double[] cc = ColorSpaceTools.getColorComponentsD_0_255(image, choosenCS, i, j);
				boolean keep = false;
				for (int k = 0; k < box.nbColors; k++) {
					if (distance.computeDistance(cc, csColors.get(k)) < box.getThreshold()) {
						keep = true;
						break;
					}
				}
				if (keep) {
					raw[idx] = BinaryIcyBufferedImage.TRUE;
				}
				idx++;
			}
		}
		Color c = box.getAverageColor();
		int ir = 255 - c.getRed();
		int ig = 255 - c.getGreen();
		int ib = 255 - c.getBlue();
		m.setColor(new Color(ir, ig, ib));
		m.setOpacity(1f);

		// moni.stop();
		// System.out.println("Filtering done (" + getThreshold() + ") : " +
		// moni.getUserElapsedTimeMilli() + " ms");
		return m;
	}

	/**
	 * Filter2.
	 * 
	 * @param boxP
	 *            the box p
	 * @param boxN
	 *            the box n
	 * @param image
	 *            the image
	 * @return the mask
	 * @throws MaskException
	 *             the mask exception
	 * @throws SignatureException
	 *             the signature exception
	 */
	private Mask filter2(ColorBox boxP, ColorBox boxN, IcyBufferedImage image) throws MaskException, SignatureException {
		if (boxP.nbColors == 0) {
			throw new MaskException("No positive color selected, filtering aborted");
		}
		if (boxN.nbColors == 0) {
			throw new MaskException("No negative color selected, filtering aborted");
		}

		// Raster raster = image.getRaster();

		DefaultVectorSignature[] pos = new DefaultVectorSignature[boxP.nbColors];
		DefaultVectorSignature[] neg = new DefaultVectorSignature[boxN.nbColors];

		for (int i = 0; i < boxP.nbColors; i++) {
			double[] ddd = ColorSpaceTools.getColorComponentsD_0_1(choosenCS, boxP.colors[i].getRed(), boxP.colors[i].getGreen(), boxP.colors[i].getBlue());
			DenseVectorSignature s = new DenseVectorSignature(ColorSpaceTools.NB_COLOR_CHANNELS);
			for (int d = 0; d < ColorSpaceTools.NB_COLOR_CHANNELS; d++) {
				s.set(d, ddd[d]);
			}
			pos[i] = s;
		}
		for (int i = 0; i < boxN.nbColors; i++) {
			double[] ddd = ColorSpaceTools.getColorComponentsD_0_1(choosenCS, boxN.colors[i].getRed(), boxN.colors[i].getGreen(), boxN.colors[i].getBlue());
			DenseVectorSignature s = new DenseVectorSignature(ColorSpaceTools.NB_COLOR_CHANNELS);
			for (int d = 0; d < ColorSpaceTools.NB_COLOR_CHANNELS; d++) {
				s.set(d, ddd[d]);
			}
			neg[i] = s;
		}

		SVMClassifier svm = new SVMClassifier();
		svm.createProblem(pos, neg);

		svm.setC(Math.pow(2, slC.getValue()));
		svm.setGamma(Math.pow(2, slGamma.getValue()));

		if (rbKernelLin.isSelected()) {
			svm.setKernel(svm_parameter.LINEAR);
		} else if (rbKernelTri.isSelected()) {
			svm.setKernel(svm_parameter.TRIANGULAR);
		} else if (rbKernelRBF.isSelected()) {
			svm.setKernel(svm_parameter.RBF);
		}

		svm.learnModel();

		Mask m = new Mask(image.getWidth(), image.getHeight(), false);
		BinaryIcyBufferedImage bin = m.getBinaryData();
		SegmentableIcyBufferedImage simg = new SegmentableIcyBufferedImage(image);

		ColorPixel col = new ColorPixel(false);
		col.setColorSpace(choosenCS);

		byte[] raw = bin.getRawData();
		int idx = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				IcyPixel pix = new IcyPixel(x, y);
				DefaultVectorSignature vs = (DefaultVectorSignature) col.extractLocalSignature(simg, pix);
				if (svm.predict(vs) > 0) {
					raw[idx] = BinaryIcyBufferedImage.TRUE;
				}
				idx++;
			}
		}

		Color c = m.getAverageColor(image);
		int ir = 255 - c.getRed();
		int ig = 255 - c.getGreen();
		int ib = 255 - c.getBlue();
		m.setColor(new Color(ir, ig, ib));
		m.setOpacity(1f);

		return m;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.PainterManager#getPainterName()
	 */
	@Override
	public String getPainterName() {
		return ColorPickerThresholdPainter.class.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		Object o = e.getSource();

		if (o == null) {
			return;
		}

		if (o instanceof JCheckBox) {
			JCheckBox c = (JCheckBox) e.getSource();

			int comp = tabbedPane.getSelectedIndex();
			ColorBox bx = null;
			if (comp == 0) {
				bx = m1ColorBox;
			} else if (comp == 1) {
				bx = m2PosColorBox;
			}

			if (c == cbShow) {
				bx.fireDisplayParametersChangeEvent();
			}

			if (c == cbAuto) {
				if (cbAuto.isSelected()) {
					bx.fireFilterParametersChangeEvent();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.PainterManagerSingletonPlugin#
	 * sequenceHasChangedAfterSettingPainter()
	 */
	@Override
	public void sequenceHasChangedAfterSettingPainter() {
		if (hasCurrentSequence()) {
			Sequence currentSequence = getCurrentSequence();
			setTitle(getName() + " - " + currentSequence.getName());
			btFilter.setEnabled(true);
			btKeepMask.setEnabled(true);
			btAsROI.setEnabled(true);
			btCancelFilter.setEnabled(true);
		} else {
			setTitle(getName());
			btFilter.setEnabled(false);
			btKeepMask.setEnabled(false);
			btAsROI.setEnabled(false);
			btCancelFilter.setEnabled(false);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.PainterManagerSingletonPlugin#
	 * sequenceHasChangedBeforeSettingPainter()
	 */
	@Override
	public void sequenceHasChangedBeforeSettingPainter() {
		// Nothing to do here
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.SingletonPlugin#sequenceWillChange()
	 */
	@Override
	public void sequenceWillChange() {
		// Nothing to do here
	}

	/**
	 * Sets the distance.
	 * 
	 * @param distance
	 *            the new distance
	 */
	public void setDistance(ColorDistance distance) {
		this.distance = distance;

		colMaxDist = (int) Math.floor(distance.getMaxDistance());
		slMajTick = (int) Math.ceil((double) colMaxDist / 100.0) * 20;
		slMinTick = (int) Math.ceil((double) slMajTick / 5.0);
		colMaxDist = (int) Math.ceil((double) colMaxDist / (double) slMajTick) * slMajTick;
		colDefaultDist = (int) Math.ceil((double) colMaxDist / 10.0);
	}

	@Override
	public Dimension getDefaultFrameDimension() {
		return null;
	}
	
	@Override
	public void fillInterface(JPanel mainPanel) {
		// KNN
		ButtonGroup bgd = new ButtonGroup();
		rbL1 = new JRadioButton("L1");
		rbL1.addActionListener(this);
		bgd.add(rbL1);
		rbL2 = new JRadioButton("L2");
		rbL2.addActionListener(this);
		bgd.add(rbL2);
		rbL1.setSelected(true);
		setDistance(new L1ColorDistance());

		slDistThreshold = new JSlider(JSlider.HORIZONTAL, 0, colMaxDist, colDefaultDist);
		slDistThreshold.addChangeListener(this);
		slDistThreshold.setMajorTickSpacing(slMajTick);
		slDistThreshold.setMinorTickSpacing(slMinTick);
		slDistThreshold.setPaintTicks(true);
		slDistThreshold.setPaintLabels(true);

		btPlus = new JButton(NherveToolbox.plusIcon);
		btPlus.addActionListener(this);
		btMinus = new JButton(NherveToolbox.minusIcon);
		btMinus.addActionListener(this);
		JPanel pmbt = GuiUtil.createPageBoxPanel(btPlus, btMinus);

		lbCurrent = new JLabel(Integer.toString(slDistThreshold.getValue()));

		JPanel boxDist = GuiUtil.createPageBoxPanel(GuiUtil.createLineBoxPanel(Box.createHorizontalGlue(), new JLabel("Distance"), Box.createHorizontalGlue()), GuiUtil.createLineBoxPanel(new Component[] { Box.createHorizontalGlue(), rbL1, Box.createHorizontalGlue(), rbL2, Box.createHorizontalGlue() }));

		JPanel thresh = GuiUtil.createLineBoxPanel(new Component[] { Box.createHorizontalGlue(), boxDist, Box.createHorizontalGlue(), slDistThreshold, Box.createHorizontalGlue(), pmbt, Box.createHorizontalGlue(), lbCurrent, Box.createHorizontalGlue() });
		thresh.setBorder(new TitledBorder("Threshold"));

		m1ColorBox = new ColorBox("Choosen colors");
		m1ColorBox.setThreshold(colDefaultDist);
		JPanel box1 = GuiUtil.createLineBoxPanel(new Component[] { Box.createHorizontalGlue(), m1ColorBox, Box.createHorizontalGlue() });

		// SVM

		m2PosColorBox = new ColorBox("Positive");
		m2NegColorBox = new ColorBox("Negative");
		JPanel box2 = GuiUtil.createLineBoxPanel(new Component[] { Box.createHorizontalGlue(), m2PosColorBox, Box.createHorizontalGlue(), m2NegColorBox, Box.createHorizontalGlue() });

		ButtonGroup kern = new ButtonGroup();
		rbKernelLin = new JRadioButton(svm.kernel_type_table[svm_parameter.LINEAR]);
		kern.add(rbKernelLin);
		rbKernelTri = new JRadioButton(svm.kernel_type_table[svm_parameter.TRIANGULAR]);
		kern.add(rbKernelTri);
		rbKernelRBF = new JRadioButton(svm.kernel_type_table[svm_parameter.RBF]);
		kern.add(rbKernelRBF);
		rbKernelTri.setSelected(true);
		JPanel box3 = GuiUtil.createLineBoxPanel(new Component[] { Box.createHorizontalGlue(), rbKernelTri, rbKernelLin, rbKernelRBF, Box.createHorizontalGlue() });

		slC = new JSlider(JSlider.HORIZONTAL, -10, 10, 0);
		slC.setMajorTickSpacing(5);
		slC.setMinorTickSpacing(1);
		slC.setSnapToTicks(true);
		slC.setPaintTicks(true);
		slC.setPaintLabels(true);
		slC.addChangeListener(this);
		ComponentUtil.setFixedSize(slC, new Dimension(275, 50));
		valC = new JLabel("C = " + Math.pow(2, slC.getValue()));
		JPanel box4 = GuiUtil.createLineBoxPanel(new Component[] { valC, Box.createHorizontalGlue(), slC });

		slGamma = new JSlider(JSlider.HORIZONTAL, -10, 10, 0);
		slGamma.setMajorTickSpacing(5);
		slGamma.setMinorTickSpacing(1);
		slGamma.setSnapToTicks(true);
		slGamma.setPaintTicks(true);
		slGamma.setPaintLabels(true);
		slGamma.addChangeListener(this);
		ComponentUtil.setFixedSize(slGamma, new Dimension(275, 50));
		valGamma = new JLabel("gamma = " + Math.pow(2, slGamma.getValue()));
		JPanel box5 = GuiUtil.createLineBoxPanel(new Component[] { valGamma, Box.createHorizontalGlue(), slGamma });

		JPanel box6 = GuiUtil.createPageBoxPanel(new Component[] { box3, box4, box5 });
		box6.setBorder(new TitledBorder("Kernel"));

		// TABBED

		JPanel tabbed1 = GuiUtil.createPageBoxPanel(new Component[] { Box.createVerticalGlue(), box1, Box.createVerticalGlue(), thresh, Box.createVerticalGlue() });
		JPanel tabbed2 = GuiUtil.createPageBoxPanel(box2, box6);

		tabbedPane = new JTabbedPane();
		tabbedPane.addTab(METHOD_1, tabbed1);
		tabbedPane.addTab(METHOD_2, tabbed2);

		// BOTTOM

		ButtonGroup bgcs = new ButtonGroup();
		rbRGB = new JRadioButton("RGB");
		rbRGB.addActionListener(this);
		bgcs.add(rbRGB);

		rbRGB.setSelected(true);
		choosenCS = ColorSpaceTools.RGB;
		rbHSV = new JRadioButton("HSV");
		rbHSV.addActionListener(this);
		bgcs.add(rbHSV);
		rbH1H2H3 = new JRadioButton("H1H2H3");
		rbH1H2H3.addActionListener(this);
		bgcs.add(rbH1H2H3);

		cbShow = new JCheckBox("Show");
		cbShow.setSelected(true);
		cbShow.addItemListener(this);

		cbAuto = new JCheckBox("Auto");
		cbAuto.addItemListener(this);

		btFilter = new JButton("Launch filtering");
		btFilter.addActionListener(this);
		btFilter.setEnabled(false);
		btCancelFilter = new JButton("Cancel filtering");
		btCancelFilter.addActionListener(this);
		btCancelFilter.setEnabled(false);
		btKeepMask = new JButton("As mask");
		btKeepMask.setEnabled(false);
		btKeepMask.addActionListener(this);
		btAsROI = new JButton("As ROI");
		btAsROI.setEnabled(false);
		btAsROI.addActionListener(this);

		JPanel buttons1 = GuiUtil.createLineBoxPanel(new Component[] { Box.createHorizontalGlue(), btFilter, btCancelFilter, btKeepMask, btAsROI, Box.createHorizontalGlue() });
		JPanel csp = GuiUtil.createLineBoxPanel(new Component[] { Box.createHorizontalGlue(), rbRGB, rbHSV, rbH1H2H3, cbShow, cbAuto, Box.createHorizontalGlue() });

		JPanel notTabbed = GuiUtil.createPageBoxPanel(csp, buttons1);

		mainPanel.add(tabbedPane);
		mainPanel.add(notTabbed);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
	 * )
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		JSlider s = (JSlider) e.getSource();
		if (s == null) {
			return;
		}
		
		if (s == slDistThreshold) {
			lbCurrent.setText(Integer.toString(slDistThreshold.getValue()));
			m1ColorBox.setThreshold(slDistThreshold.getValue());
		}
		
		if (s == slC) {
			valC.setText("C = " + Math.pow(2, slC.getValue()));
			m2PosColorBox.fireFilterParametersChangeEvent();
		}
		
		if (s == slGamma) {
			valGamma.setText("gamma = " + Math.pow(2, slGamma.getValue()));
			m2PosColorBox.fireFilterParametersChangeEvent();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.nherve.toolbox.plugin.SingletonPlugin#stopInterface()
	 */
	@Override
	public void stopInterface() {
	}

}
