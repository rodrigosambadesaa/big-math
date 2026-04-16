package ch.obermuhlner.math.big.example;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import ch.obermuhlner.math.big.BigComplex;
import ch.obermuhlner.math.big.BigComplexMath;
import ch.obermuhlner.math.big.BigDecimalMath;

public class BigMathCalculatorApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Keep default LAF.
            }
            new BigMathCalculatorApp().createAndShow();
        });
    }

    private final JTextField expressionField = new JTextField();
    private final JSpinner precisionSpinner = new JSpinner(new SpinnerNumberModel(50, 5, 1000, 5));
    private final JComboBox<String> modeCombo = new JComboBox<>(new String[] { "Complejo", "Real" });
    private final JTextArea resultArea = new JTextArea();
    private final DefaultListModel<String> historyModel = new DefaultListModel<String>();
    private final JList<String> historyList = new JList<String>(historyModel);

    private BigComplex lastResult = BigComplex.ZERO;

    private void createAndShow() {
        JFrame frame = new JFrame("Big-Math Calculator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        expressionField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        expressionField.addActionListener(this::evaluateFromInput);
        north.add(new JLabel("Expresion:"), BorderLayout.WEST);
        north.add(expressionField, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(1, 8, 6, 6));
        controls.add(new JLabel("Precision"));
        controls.add(precisionSpinner);
        controls.add(new JLabel("Modo"));
        controls.add(modeCombo);

        JButton evalButton = new JButton("Evaluar");
        evalButton.addActionListener(this::evaluateFromInput);
        controls.add(evalButton);

        JButton clearButton = new JButton("Limpiar");
        clearButton.addActionListener(e -> expressionField.setText(""));
        controls.add(clearButton);

        JButton ansButton = new JButton("ans");
        ansButton.addActionListener(e -> insertToken("ans"));
        controls.add(ansButton);

        JButton helpButton = new JButton("Ayuda");
        helpButton.addActionListener(e -> showHelp());
        controls.add(helpButton);

        north.add(controls, BorderLayout.SOUTH);
        frame.add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        center.add(createPadPanel());
        center.add(createResultPanel());
        frame.add(center, BorderLayout.CENTER);

        historyList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = historyList.getSelectedValue();
                if (selected != null) {
                    int eqIndex = selected.indexOf(" = ");
                    if (eqIndex > 0) {
                        expressionField.setText(selected.substring(0, eqIndex));
                    }
                }
            }
        });

        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(900, 160));
        historyScroll.setBorder(BorderFactory.createTitledBorder("Historial"));
        frame.add(historyScroll, BorderLayout.SOUTH);

        expressionField.setText("(x+2)^4");
        frame.setSize(1100, 760);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createPadPanel() {
        JPanel panel = new JPanel(new GridLayout(8, 6, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Teclado"));

        String[] buttons = {
                "7", "8", "9", "/", "(", ")",
                "4", "5", "6", "*", "^", "!",
                "1", "2", "3", "-", "pi", "e",
                "0", ".", ",", "+", "i", "ans",
                "x", "y", "=", "sin(", "cos(", "tan(",
                "log(", "exp(", "sqrt(", "asin(", "acos(", "atan(",
                "sinh(", "cosh(", "tanh(", "pow(", "root(", "abs(",
                "arg(", "conj(", "re(", "im(", "gamma(", "factorial("
        };

        for (String label : buttons) {
            JButton button = new JButton(label);
            button.addActionListener(e -> {
                if ("=".equals(label)) {
                    evaluateFromInput(e);
                } else {
                    insertToken(label);
                }
            });
            panel.add(button);
        }

        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Resultado"));

        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JComboBox<String> samples = new JComboBox<String>(new String[] {
                "Ejemplos rapidos",
                "(x+2)^4",
                "sqrt(2)",
                "pow(2, 100)",
                "gamma(0.5)",
                "exp(i*pi)+1",
                "(2+3i)*(4-5i)",
                "root(-8,3)",
                "atan2(1,1)",
                "abs(3+4i)",
                "x^2-2",
                "sin(x)"
        });
        samples.addActionListener(e -> {
            String selected = (String) samples.getSelectedItem();
            if (selected != null && !"Ejemplos rapidos".equals(selected)) {
                expressionField.setText(selected);
                expressionField.requestFocus();
            }
        });
        panel.add(samples, BorderLayout.NORTH);

        JPanel advanced = new JPanel(new GridLayout(2, 2, 6, 6));
        advanced.setBorder(BorderFactory.createTitledBorder("Algebra / Graficas"));

        JButton expandButton = new JButton("Expandir polinomio");
        expandButton.addActionListener(e -> expandPolynomialFromInput());
        advanced.add(expandButton);

        JButton solveButton = new JButton("Resolver f(x)=0");
        solveButton.addActionListener(e -> solveEquationDialog());
        advanced.add(solveButton);

        JButton plot2dButton = new JButton("Graficar 2D");
        plot2dButton.addActionListener(e -> plot2DDialog());
        advanced.add(plot2dButton);

        JButton plot3dButton = new JButton("Graficar 3D");
        plot3dButton.addActionListener(e -> plot3DDialog());
        advanced.add(plot3dButton);

        panel.add(advanced, BorderLayout.SOUTH);

        return panel;
    }

    private void insertToken(String token) {
        expressionField.replaceSelection(token);
        expressionField.requestFocus();
    }

    private void evaluateFromInput(ActionEvent event) {
        String expression = expressionField.getText().trim();
        if (expression.isEmpty()) {
            return;
        }

        MathContext mc = currentMathContext();
        boolean realMode = "Real".equals(modeCombo.getSelectedItem());

        try {
            ExpressionEngine engine = new ExpressionEngine(mc, lastResult, new HashMap<String, BigComplex>());
            BigComplex value = engine.evaluate(expression);
            if (realMode && value.im.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("El modo Real solo permite resultados con parte imaginaria 0.");
            }

            lastResult = value;
            String rendered = formatComplex(value);
            String output = "expresion: " + expression + "\n"
                    + "precision: " + mc.getPrecision() + "\n"
                    + "resultado: " + rendered;
            resultArea.setText(output);
            historyModel.add(0, expression + " = " + rendered);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error de calculo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void expandPolynomialFromInput() {
        String expression = expressionField.getText().trim();
        if (expression.isEmpty()) {
            return;
        }

        try {
            Polynomial polynomial = PolynomialParser.parse(expression, currentMathContext());
            String expanded = polynomial.toPrettyString();
            String output = "expresion: " + expression + "\n"
                    + "expansion: " + expanded;
            resultArea.setText(output);
            historyModel.add(0, "expand(" + expression + ") = " + expanded);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo expandir. Use solo expresiones polinomicas en x (ej: (x+2)^4).\n" + ex.getMessage(),
                    "Error de algebra", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solveEquationDialog() {
        String defaultText = expressionField.getText().trim();
        if (defaultText.isEmpty()) {
            defaultText = "x^2-2=0";
        }

        String equation = JOptionPane.showInputDialog(null,
                "Ecuacion en x (ej: x^2-2=0 o sin(x)-0.5=0)",
                defaultText);
        if (equation == null || equation.trim().isEmpty()) {
            return;
        }

        try {
            EquationSolver solver = new EquationSolver(currentMathContext(), lastResult);
            List<Double> roots = solver.solve(equation.trim(), -25.0, 25.0);
            if (roots.isEmpty()) {
                resultArea.setText("No se encontraron raices reales en [-25, 25] para: " + equation);
                historyModel.add(0, "roots(" + equation + ") = none");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("ecuacion: ").append(equation).append('\n');
            sb.append("raices reales aproximadas:\n");
            for (int i = 0; i < roots.size(); i++) {
                sb.append("x").append(i + 1).append(" ~= ").append(roots.get(i)).append('\n');
            }

            resultArea.setText(sb.toString());
            historyModel.add(0, "roots(" + equation + ") = " + roots);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error al resolver", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void plot2DDialog() {
        String expression = JOptionPane.showInputDialog(null,
                "f(x) para graficar en 2D",
                expressionField.getText().trim().isEmpty() ? "sin(x)" : expressionField.getText().trim());
        if (expression == null || expression.trim().isEmpty()) {
            return;
        }

        Double minX = askDouble("x minimo", -10.0);
        if (minX == null) {
            return;
        }
        Double maxX = askDouble("x maximo", 10.0);
        if (maxX == null) {
            return;
        }
        if (maxX <= minX) {
            JOptionPane.showMessageDialog(null, "x maximo debe ser mayor que x minimo", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Plot2DFrame(expression.trim(), minX.doubleValue(), maxX.doubleValue(), currentMathContext(), lastResult)
                .setVisible(true);
    }

    private void plot3DDialog() {
        String expression = JOptionPane.showInputDialog(null,
                "z = f(x,y) para graficar en 3D",
                "sin(x)*cos(y)");
        if (expression == null || expression.trim().isEmpty()) {
            return;
        }

        Double min = askDouble("minimo de x e y", -5.0);
        if (min == null) {
            return;
        }
        Double max = askDouble("maximo de x e y", 5.0);
        if (max == null) {
            return;
        }
        if (max <= min) {
            JOptionPane.showMessageDialog(null, "maximo debe ser mayor que minimo", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Plot3DFrame(expression.trim(), min.doubleValue(), max.doubleValue(), currentMathContext(), lastResult)
                .setVisible(true);
    }

    private Double askDouble(String label, double defaultValue) {
        String value = JOptionPane.showInputDialog(null, label, Double.toString(defaultValue));
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(Double.parseDouble(value.trim()));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Numero invalido: " + value, "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private MathContext currentMathContext() {
        int precision = ((Integer) precisionSpinner.getValue()).intValue();
        return new MathContext(precision, RoundingMode.HALF_UP);
    }

    private static String formatComplex(BigComplex value) {
        BigDecimal re = value.re.stripTrailingZeros();
        BigDecimal im = value.im.stripTrailingZeros();

        if (im.compareTo(BigDecimal.ZERO) == 0) {
            return re.toPlainString();
        }
        if (re.compareTo(BigDecimal.ZERO) == 0) {
            return im.toPlainString() + "i";
        }
        String sign = im.signum() >= 0 ? " + " : " - ";
        BigDecimal absIm = im.abs();
        return re.toPlainString() + sign + absIm.toPlainString() + "i";
    }

    private void showHelp() {
        String help = "Operadores: +  -  *  /  ^  !\n"
                + "Variables: x, y\n"
                + "Multiplicacion implicita: 2pi, 3i, (1+i)(2-i), 3x\n"
                + "Constantes: pi, e, i, ans\n"
                + "Funciones 1 arg: sin cos tan asin acos atan sinh cosh tanh exp log sqrt gamma factorial abs arg conj re im\n"
                + "Funciones 2 args: pow(a,b), root(a,n), atan2(y,x)\n"
                + "Algebra: Expandir polinomio (ej: (x+2)^4)\n"
                + "Ecuaciones: Resolver f(x)=0\n"
                + "Graficas: 2D (f(x)) y 3D (z=f(x,y))";
        JOptionPane.showMessageDialog(null, help, "Ayuda", JOptionPane.INFORMATION_MESSAGE);
    }

    private interface RealFunction {
        double apply(double x);
    }

    private static final class EquationSolver {
        private final MathContext mc;
        private final BigComplex ans;

        private EquationSolver(MathContext mc, BigComplex ans) {
            this.mc = mc;
            this.ans = ans;
        }

        private List<Double> solve(String equation, double minX, double maxX) {
            String[] sides = splitEquation(equation);
            String left = sides[0];
            String right = sides[1];

            RealFunction f = x -> evaluateReal(left, right, x);
            List<Double> roots = new ArrayList<Double>();

            int steps = 400;
            double step = (maxX - minX) / steps;
            double prevX = minX;
            double prevY = f.apply(prevX);

            for (int i = 1; i <= steps; i++) {
                double x = minX + i * step;
                double y = f.apply(x);

                if (!Double.isFinite(prevY) || !Double.isFinite(y)) {
                    prevX = x;
                    prevY = y;
                    continue;
                }

                if (Math.abs(y) < 1.0E-10) {
                    addUnique(roots, x);
                } else if (prevY == 0.0 || prevY * y < 0.0) {
                    double root = bisect(f, prevX, x);
                    addUnique(roots, root);
                }

                prevX = x;
                prevY = y;
            }

            return roots;
        }

        private String[] splitEquation(String equation) {
            int idx = equation.indexOf('=');
            if (idx < 0) {
                return new String[] { equation, "0" };
            }
            return new String[] { equation.substring(0, idx), equation.substring(idx + 1) };
        }

        private double evaluateReal(String left, String right, double x) {
            Map<String, BigComplex> vars = new HashMap<String, BigComplex>();
            vars.put("x", BigComplex.valueOf(BigDecimal.valueOf(x)));
            ExpressionEngine engine = new ExpressionEngine(mc, ans, vars);
            BigComplex l = engine.evaluate(left);
            BigComplex r = engine.evaluate(right);
            BigComplex diff = l.subtract(r, mc);
            if (diff.im.abs().compareTo(BigDecimal.valueOf(1.0E-8)) > 0) {
                throw new IllegalArgumentException("La ecuacion debe evaluarse en reales para resolver f(x)=0");
            }
            return diff.re.doubleValue();
        }

        private double bisect(RealFunction f, double a, double b) {
            double fa = f.apply(a);
            double fb = f.apply(b);
            if (fa == 0.0) {
                return a;
            }
            if (fb == 0.0) {
                return b;
            }
            for (int i = 0; i < 80; i++) {
                double m = (a + b) / 2.0;
                double fm = f.apply(m);
                if (Math.abs(fm) < 1.0E-13) {
                    return m;
                }
                if (fa * fm <= 0.0) {
                    b = m;
                    fb = fm;
                } else {
                    a = m;
                    fa = fm;
                }
            }
            return (a + b) / 2.0;
        }

        private void addUnique(List<Double> roots, double root) {
            for (Double existing : roots) {
                if (Math.abs(existing.doubleValue() - root) < 1.0E-6) {
                    return;
                }
            }
            roots.add(Double.valueOf(root));
        }
    }

    private static final class Plot2DFrame extends JFrame {
        private Plot2DFrame(String expression, double minX, double maxX, MathContext mc, BigComplex ans) {
            super("Grafica 2D: " + expression);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(860, 560);
            setLocationByPlatform(true);
            add(new Plot2DPanel(expression, minX, maxX, mc, ans));
        }
    }

    private static final class Plot2DPanel extends JPanel {
        private final String expression;
        private final double minX;
        private final double maxX;
        private final MathContext mc;
        private final BigComplex ans;

        private Plot2DPanel(String expression, double minX, double maxX, MathContext mc, BigComplex ans) {
            this.expression = expression;
            this.minX = minX;
            this.maxX = maxX;
            this.mc = mc;
            this.ans = ans;
            setBackground(new Color(245, 248, 252));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int pad = 50;

            double[] ys = new double[Math.max(300, w - 2 * pad)];
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < ys.length; i++) {
                double x = minX + (maxX - minX) * i / (ys.length - 1.0);
                double y = evaluate(expression, x, 0.0, mc, ans);
                ys[i] = y;
                if (Double.isFinite(y)) {
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }

            if (!Double.isFinite(minY) || !Double.isFinite(maxY) || minY == maxY) {
                minY = -1.0;
                maxY = 1.0;
            }

            g2.setColor(new Color(220, 228, 240));
            g2.fillRect(pad, pad, w - 2 * pad, h - 2 * pad);

            g2.setColor(new Color(120, 130, 150));
            int xAxis = mapY(0.0, minY, maxY, h, pad);
            int yAxis = mapX(0.0, minX, maxX, w, pad);
            g2.drawLine(pad, xAxis, w - pad, xAxis);
            g2.drawLine(yAxis, pad, yAxis, h - pad);

            g2.setColor(new Color(26, 98, 196));
            g2.setStroke(new BasicStroke(2f));
            int prevX = 0;
            int prevY = 0;
            boolean prevValid = false;
            for (int i = 0; i < ys.length; i++) {
                double x = minX + (maxX - minX) * i / (ys.length - 1.0);
                double y = ys[i];
                if (!Double.isFinite(y)) {
                    prevValid = false;
                    continue;
                }
                int sx = mapX(x, minX, maxX, w, pad);
                int sy = mapY(y, minY, maxY, h, pad);
                if (prevValid) {
                    g2.drawLine(prevX, prevY, sx, sy);
                }
                prevX = sx;
                prevY = sy;
                prevValid = true;
            }

            g2.setColor(new Color(20, 20, 20));
            g2.drawString("x in [" + minX + ", " + maxX + "]", pad, h - 14);
            g2.drawString("f(x) = " + expression, pad, 20);
        }

        private int mapX(double x, double minX, double maxX, int width, int pad) {
            return (int) Math.round(pad + (x - minX) * (width - 2.0 * pad) / (maxX - minX));
        }

        private int mapY(double y, double minY, double maxY, int height, int pad) {
            return (int) Math.round(height - pad - (y - minY) * (height - 2.0 * pad) / (maxY - minY));
        }
    }

    private static final class Plot3DFrame extends JFrame {
        private Plot3DFrame(String expression, double min, double max, MathContext mc, BigComplex ans) {
            super("Grafica 3D: z = " + expression);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(920, 650);
            setLocationByPlatform(true);
            add(new Plot3DPanel(expression, min, max, mc, ans));
        }
    }

    private static final class Plot3DPanel extends JPanel {
        private final String expression;
        private final double min;
        private final double max;
        private final MathContext mc;
        private final BigComplex ans;

        private Plot3DPanel(String expression, double min, double max, MathContext mc, BigComplex ans) {
            this.expression = expression;
            this.min = min;
            this.max = max;
            this.mc = mc;
            this.ans = ans;
            setBackground(new Color(246, 249, 252));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2 + 60;

            int grid = 30;
            double[][] z = new double[grid + 1][grid + 1];
            double zMin = Double.POSITIVE_INFINITY;
            double zMax = Double.NEGATIVE_INFINITY;

            for (int i = 0; i <= grid; i++) {
                for (int j = 0; j <= grid; j++) {
                    double x = min + (max - min) * i / (double) grid;
                    double y = min + (max - min) * j / (double) grid;
                    double value = evaluate(expression, x, y, mc, ans);
                    z[i][j] = value;
                    if (Double.isFinite(value)) {
                        zMin = Math.min(zMin, value);
                        zMax = Math.max(zMax, value);
                    }
                }
            }

            if (!Double.isFinite(zMin) || !Double.isFinite(zMax) || zMin == zMax) {
                zMin = -1.0;
                zMax = 1.0;
            }

            double scaleXY = 30.0 / Math.max(1.0, (max - min));
            double scaleZ = 100.0 / Math.max(1.0, (zMax - zMin));

            g2.setColor(new Color(73, 97, 130));
            for (int i = 0; i <= grid; i++) {
                for (int j = 0; j <= grid; j++) {
                    if (i < grid) {
                        drawProjectedLine(g2, i, j, i + 1, j, z, grid, cx, cy, scaleXY, scaleZ);
                    }
                    if (j < grid) {
                        drawProjectedLine(g2, i, j, i, j + 1, z, grid, cx, cy, scaleXY, scaleZ);
                    }
                }
            }

            g2.setColor(new Color(20, 20, 20));
            g2.drawString("z = " + expression, 14, 20);
            g2.drawString("x,y in [" + min + ", " + max + "]", 14, 38);
        }

        private void drawProjectedLine(Graphics2D g2, int i1, int j1, int i2, int j2,
                double[][] z, int grid, int cx, int cy, double scaleXY, double scaleZ) {
            double x1 = lerp(min, max, i1 / (double) grid);
            double y1 = lerp(min, max, j1 / (double) grid);
            double x2 = lerp(min, max, i2 / (double) grid);
            double y2 = lerp(min, max, j2 / (double) grid);

            double z1 = z[i1][j1];
            double z2 = z[i2][j2];
            if (!Double.isFinite(z1) || !Double.isFinite(z2)) {
                return;
            }

            int sx1 = (int) Math.round(cx + (x1 - y1) * scaleXY * 0.85);
            int sy1 = (int) Math.round(cy + (x1 + y1) * scaleXY * 0.45 - z1 * scaleZ);
            int sx2 = (int) Math.round(cx + (x2 - y2) * scaleXY * 0.85);
            int sy2 = (int) Math.round(cy + (x2 + y2) * scaleXY * 0.45 - z2 * scaleZ);

            g2.drawLine(sx1, sy1, sx2, sy2);
        }

        private double lerp(double a, double b, double t) {
            return a + (b - a) * t;
        }
    }

    private static double evaluate(String expression, double x, double y, MathContext mc, BigComplex ans) {
        try {
            Map<String, BigComplex> vars = new HashMap<String, BigComplex>();
            vars.put("x", BigComplex.valueOf(BigDecimal.valueOf(x)));
            vars.put("y", BigComplex.valueOf(BigDecimal.valueOf(y)));
            ExpressionEngine engine = new ExpressionEngine(mc, ans, vars);
            BigComplex result = engine.evaluate(expression);
            if (result.im.abs().compareTo(BigDecimal.valueOf(1.0E-7)) > 0) {
                return Double.NaN;
            }
            return result.re.doubleValue();
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private static final class Polynomial {
        private final List<BigDecimal> coefficients;

        private Polynomial(List<BigDecimal> coefficients) {
            this.coefficients = trim(coefficients);
        }

        private static Polynomial constant(BigDecimal value) {
            List<BigDecimal> list = new ArrayList<BigDecimal>();
            list.add(value);
            return new Polynomial(list);
        }

        private static Polynomial variable() {
            List<BigDecimal> list = new ArrayList<BigDecimal>();
            list.add(BigDecimal.ZERO);
            list.add(BigDecimal.ONE);
            return new Polynomial(list);
        }

        private Polynomial add(Polynomial other) {
            int max = Math.max(coefficients.size(), other.coefficients.size());
            List<BigDecimal> result = new ArrayList<BigDecimal>();
            for (int i = 0; i < max; i++) {
                BigDecimal a = i < coefficients.size() ? coefficients.get(i) : BigDecimal.ZERO;
                BigDecimal b = i < other.coefficients.size() ? other.coefficients.get(i) : BigDecimal.ZERO;
                result.add(a.add(b));
            }
            return new Polynomial(result);
        }

        private Polynomial subtract(Polynomial other) {
            int max = Math.max(coefficients.size(), other.coefficients.size());
            List<BigDecimal> result = new ArrayList<BigDecimal>();
            for (int i = 0; i < max; i++) {
                BigDecimal a = i < coefficients.size() ? coefficients.get(i) : BigDecimal.ZERO;
                BigDecimal b = i < other.coefficients.size() ? other.coefficients.get(i) : BigDecimal.ZERO;
                result.add(a.subtract(b));
            }
            return new Polynomial(result);
        }

        private Polynomial multiply(Polynomial other) {
            List<BigDecimal> result = new ArrayList<BigDecimal>();
            for (int i = 0; i < coefficients.size() + other.coefficients.size(); i++) {
                result.add(BigDecimal.ZERO);
            }

            for (int i = 0; i < coefficients.size(); i++) {
                for (int j = 0; j < other.coefficients.size(); j++) {
                    BigDecimal old = result.get(i + j);
                    result.set(i + j, old.add(coefficients.get(i).multiply(other.coefficients.get(j))));
                }
            }
            return new Polynomial(result);
        }

        private Polynomial pow(int exponent) {
            if (exponent < 0) {
                throw new IllegalArgumentException("Exponente negativo no soportado para polinomios");
            }
            Polynomial result = constant(BigDecimal.ONE);
            Polynomial base = this;
            int e = exponent;
            while (e > 0) {
                if ((e & 1) == 1) {
                    result = result.multiply(base);
                }
                if (e > 1) {
                    base = base.multiply(base);
                }
                e >>= 1;
            }
            return result;
        }

        private int degree() {
            return coefficients.size() - 1;
        }

        private BigDecimal constantTerm() {
            return coefficients.get(0);
        }

        private String toPrettyString() {
            StringBuilder sb = new StringBuilder();
            for (int d = degree(); d >= 0; d--) {
                BigDecimal c = coefficients.get(d);
                if (c.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                if (sb.length() > 0) {
                    sb.append(c.signum() >= 0 ? " + " : " - ");
                } else if (c.signum() < 0) {
                    sb.append('-');
                }

                BigDecimal abs = c.abs();
                boolean showCoeff = d == 0 || abs.compareTo(BigDecimal.ONE) != 0;
                if (showCoeff) {
                    sb.append(formatDecimal(abs));
                }
                if (d > 0) {
                    if (showCoeff) {
                        sb.append('*');
                    }
                    sb.append('x');
                    if (d > 1) {
                        sb.append('^').append(d);
                    }
                }
            }

            if (sb.length() == 0) {
                return "0";
            }
            return sb.toString();
        }

        private static List<BigDecimal> trim(List<BigDecimal> original) {
            int last = original.size() - 1;
            while (last > 0 && original.get(last).compareTo(BigDecimal.ZERO) == 0) {
                last--;
            }
            List<BigDecimal> trimmed = new ArrayList<BigDecimal>();
            for (int i = 0; i <= last; i++) {
                trimmed.add(original.get(i));
            }
            return trimmed;
        }
    }

    private static final class PolynomialParser {
        private final MathContext mc;
        private List<Token> tokens;
        private int index;

        private PolynomialParser(MathContext mc) {
            this.mc = mc;
        }

        private static Polynomial parse(String expression, MathContext mc) {
            PolynomialParser parser = new PolynomialParser(mc);
            return parser.parseInternal(expression);
        }

        private Polynomial parseInternal(String expression) {
            tokens = tokenize(expression);
            index = 0;
            Polynomial polynomial = parseExpression();
            if (!peek(TokenType.EOF)) {
                throw new IllegalArgumentException("Token inesperado: " + current().text);
            }
            return polynomial;
        }

        private Polynomial parseExpression() {
            Polynomial value = parseTerm();
            while (match("+", "-")) {
                String op = previous().text;
                Polynomial right = parseTerm();
                value = "+".equals(op) ? value.add(right) : value.subtract(right);
            }
            return value;
        }

        private Polynomial parseTerm() {
            Polynomial value = parsePower();
            while (match("*")) {
                value = value.multiply(parsePower());
            }
            return value;
        }

        private Polynomial parsePower() {
            Polynomial base = parseUnary();
            if (match("^")) {
                Polynomial exponentPoly = parsePower();
                if (exponentPoly.degree() != 0) {
                    throw new IllegalArgumentException("El exponente debe ser constante entera no negativa");
                }
                BigDecimal exp = exponentPoly.constantTerm();
                if (exp.scale() > 0 || exp.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("El exponente debe ser entero no negativo");
                }
                int e = exp.intValueExact();
                return base.pow(e);
            }
            return base;
        }

        private Polynomial parseUnary() {
            if (match("+")) {
                return parseUnary();
            }
            if (match("-")) {
                return Polynomial.constant(BigDecimal.ZERO).subtract(parseUnary());
            }
            return parsePrimary();
        }

        private Polynomial parsePrimary() {
            if (match(TokenType.NUMBER)) {
                return Polynomial.constant(BigDecimalMath.toBigDecimal(previous().text, mc));
            }
            if (match(TokenType.IDENTIFIER)) {
                String id = previous().text.toLowerCase(Locale.ROOT);
                if (!"x".equals(id)) {
                    throw new IllegalArgumentException("Solo se permite variable x en expansion");
                }
                return Polynomial.variable();
            }
            if (match("(")) {
                Polynomial inside = parseExpression();
                consume(")", "Falta ')' en expresion polinomica");
                return inside;
            }
            throw new IllegalArgumentException("Expresion polinomica invalida");
        }

        private List<Token> tokenize(String source) {
            List<Token> base = new ArrayList<Token>();
            int pos = 0;
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }
                if ("+-*^()".indexOf(c) >= 0) {
                    base.add(new Token(TokenType.OPERATOR, String.valueOf(c)));
                    pos++;
                    continue;
                }
                if (Character.isDigit(c) || c == '.') {
                    int start = pos;
                    pos = consumeNumber(source, pos);
                    base.add(new Token(TokenType.NUMBER, source.substring(start, pos)));
                    continue;
                }
                if (Character.isLetter(c) || c == '_') {
                    int start = pos;
                    pos++;
                    while (pos < source.length()
                            && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
                        pos++;
                    }
                    base.add(new Token(TokenType.IDENTIFIER, source.substring(start, pos)));
                    continue;
                }
                throw new IllegalArgumentException("Caracter no permitido en polinomio: '" + c + "'");
            }

            List<Token> expanded = new ArrayList<Token>();
            for (int i = 0; i < base.size(); i++) {
                Token current = base.get(i);
                expanded.add(current);
                if (i + 1 < base.size() && needsImplicitMultiplication(current, base.get(i + 1))) {
                    expanded.add(new Token(TokenType.OPERATOR, "*"));
                }
            }
            expanded.add(new Token(TokenType.EOF, ""));
            return expanded;
        }

        private int consumeNumber(String source, int start) {
            int pos = start;
            boolean seenDot = false;
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (Character.isDigit(c)) {
                    pos++;
                    continue;
                }
                if (c == '.') {
                    if (seenDot) {
                        break;
                    }
                    seenDot = true;
                    pos++;
                    continue;
                }
                break;
            }
            return pos;
        }

        private boolean needsImplicitMultiplication(Token current, Token next) {
            boolean left = current.type == TokenType.NUMBER || current.type == TokenType.IDENTIFIER
                    || ")".equals(current.text);
            boolean right = next.type == TokenType.NUMBER || next.type == TokenType.IDENTIFIER || "(".equals(next.text);
            return left && right;
        }

        private boolean match(String... symbols) {
            for (String symbol : symbols) {
                if (check(symbol)) {
                    advance();
                    return true;
                }
            }
            return false;
        }

        private boolean match(TokenType type) {
            if (peek(type)) {
                advance();
                return true;
            }
            return false;
        }

        private void consume(String symbol, String message) {
            if (!check(symbol)) {
                throw new IllegalArgumentException(message);
            }
            advance();
        }

        private boolean check(String symbol) {
            return symbol.equals(current().text);
        }

        private boolean peek(TokenType type) {
            return current().type == type;
        }

        private Token advance() {
            if (!peek(TokenType.EOF)) {
                index++;
            }
            return previous();
        }

        private Token current() {
            return tokens.get(index);
        }

        private Token previous() {
            return tokens.get(index - 1);
        }
    }

    private static String formatDecimal(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }

    private static final class ExpressionEngine {
        private final MathContext mc;
        private final BigComplex ans;
        private final Map<String, BigComplex> variables;

        private List<Token> tokens;
        private int index;

        private ExpressionEngine(MathContext mc, BigComplex ans, Map<String, BigComplex> variables) {
            this.mc = mc;
            this.ans = ans;
            this.variables = variables;
        }

        private BigComplex evaluate(String expression) {
            tokens = tokenize(expression);
            index = 0;
            BigComplex result = parseExpression();
            if (!peek(TokenType.EOF)) {
                throw error("Token inesperado: " + current().text);
            }
            return result.round(mc);
        }

        private BigComplex parseExpression() {
            BigComplex value = parseTerm();
            while (match("+", "-")) {
                String op = previous().text;
                BigComplex right = parseTerm();
                value = "+".equals(op) ? value.add(right, mc) : value.subtract(right, mc);
            }
            return value;
        }

        private BigComplex parseTerm() {
            BigComplex value = parsePower();
            while (match("*", "/")) {
                String op = previous().text;
                BigComplex right = parsePower();
                value = "*".equals(op) ? value.multiply(right, mc) : value.divide(right, mc);
            }
            return value;
        }

        private BigComplex parsePower() {
            BigComplex base = parseUnary();
            if (match("^")) {
                base = BigComplexMath.pow(base, parsePower(), mc);
            }
            return base;
        }

        private BigComplex parseUnary() {
            if (match("+")) {
                return parseUnary();
            }
            if (match("-")) {
                return parseUnary().negate();
            }
            return parsePostfix();
        }

        private BigComplex parsePostfix() {
            BigComplex value = parsePrimary();
            while (match("!")) {
                value = BigComplexMath.factorial(value, mc);
            }
            return value;
        }

        private BigComplex parsePrimary() {
            if (match(TokenType.NUMBER)) {
                return BigComplex.valueOf(BigDecimalMath.toBigDecimal(previous().text, mc));
            }
            if (match(TokenType.IDENTIFIER)) {
                String identifier = previous().text.toLowerCase(Locale.ROOT);
                if (match("(")) {
                    List<BigComplex> arguments = new ArrayList<BigComplex>();
                    if (!check(")")) {
                        do {
                            arguments.add(parseExpression());
                        } while (match(","));
                    }
                    consume(")", "Se esperaba ')' al cerrar llamada de funcion");
                    return evaluateFunction(identifier, arguments);
                }
                return resolveSymbol(identifier);
            }
            if (match("(")) {
                BigComplex value = parseExpression();
                consume(")", "Se esperaba ')' al cerrar parentesis");
                return value;
            }
            throw error("Se esperaba numero, variable, funcion o parentesis");
        }

        private BigComplex resolveSymbol(String identifier) {
            if (variables.containsKey(identifier)) {
                return variables.get(identifier);
            }
            if ("pi".equals(identifier)) {
                return BigComplex.valueOf(BigDecimalMath.pi(mc));
            }
            if ("e".equals(identifier)) {
                return BigComplex.valueOf(BigDecimalMath.e(mc));
            }
            if ("i".equals(identifier)) {
                return BigComplex.I;
            }
            if ("ans".equals(identifier)) {
                return ans;
            }
            throw error("Constante o variable desconocida: " + identifier);
        }

        private BigComplex evaluateFunction(String name, List<BigComplex> args) {
            switch (name) {
                case "sin":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.sin(args.get(0), mc);
                case "cos":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.cos(args.get(0), mc);
                case "tan":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.tan(args.get(0), mc);
                case "asin":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.asin(args.get(0), mc);
                case "acos":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.acos(args.get(0), mc);
                case "atan":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.atan(args.get(0), mc);
                case "sinh":
                    checkArgCount(name, args, 1);
                    return sinh(args.get(0));
                case "cosh":
                    checkArgCount(name, args, 1);
                    return cosh(args.get(0));
                case "tanh":
                    checkArgCount(name, args, 1);
                    return tanh(args.get(0));
                case "exp":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.exp(args.get(0), mc);
                case "log":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.log(args.get(0), mc);
                case "sqrt":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.sqrt(args.get(0), mc);
                case "gamma":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.gamma(args.get(0), mc);
                case "factorial":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.factorial(args.get(0), mc);
                case "abs":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigComplexMath.abs(args.get(0), mc));
                case "arg":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigComplexMath.angle(args.get(0), mc));
                case "conj":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.conjugate(args.get(0));
                case "re":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(args.get(0).re);
                case "im":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(args.get(0).im);
                case "pow":
                    checkArgCount(name, args, 2);
                    return BigComplexMath.pow(args.get(0), args.get(1), mc);
                case "root":
                    checkArgCount(name, args, 2);
                    return BigComplexMath.root(args.get(0), args.get(1), mc);
                case "atan2":
                    checkArgCount(name, args, 2);
                    BigDecimal y = requireReal(args.get(0), "atan2");
                    BigDecimal x = requireReal(args.get(1), "atan2");
                    return BigComplex.valueOf(BigDecimalMath.atan2(y, x, mc));
                default:
                    throw error("Funcion desconocida: " + name);
            }
        }

        private BigComplex sinh(BigComplex x) {
            BigComplex exp = BigComplexMath.exp(x, mc);
            BigComplex expNeg = BigComplexMath.exp(x.negate(), mc);
            return exp.subtract(expNeg, mc).divide(BigDecimal.valueOf(2), mc);
        }

        private BigComplex cosh(BigComplex x) {
            BigComplex exp = BigComplexMath.exp(x, mc);
            BigComplex expNeg = BigComplexMath.exp(x.negate(), mc);
            return exp.add(expNeg, mc).divide(BigDecimal.valueOf(2), mc);
        }

        private BigComplex tanh(BigComplex x) {
            return sinh(x).divide(cosh(x), mc);
        }

        private BigDecimal requireReal(BigComplex value, String functionName) {
            if (value.im.compareTo(BigDecimal.ZERO) != 0) {
                throw error(functionName + " requiere argumentos reales");
            }
            return value.re;
        }

        private void checkArgCount(String name, List<BigComplex> args, int expected) {
            if (args.size() != expected) {
                throw error(
                        "La funcion " + name + " espera " + expected + " argumentos (recibidos: " + args.size() + ")");
            }
        }

        private List<Token> tokenize(String source) {
            List<Token> base = new ArrayList<Token>();
            int pos = 0;
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }
                if (isOperatorChar(c)) {
                    base.add(new Token(TokenType.OPERATOR, String.valueOf(c)));
                    pos++;
                    continue;
                }
                if (Character.isDigit(c) || c == '.') {
                    int start = pos;
                    pos = consumeNumber(source, pos);
                    base.add(new Token(TokenType.NUMBER, source.substring(start, pos)));
                    continue;
                }
                if (Character.isLetter(c) || c == '_') {
                    int start = pos;
                    pos++;
                    while (pos < source.length()) {
                        char ch = source.charAt(pos);
                        if (Character.isLetterOrDigit(ch) || ch == '_') {
                            pos++;
                        } else {
                            break;
                        }
                    }
                    base.add(new Token(TokenType.IDENTIFIER, source.substring(start, pos)));
                    continue;
                }
                throw error("Caracter no valido: '" + c + "'");
            }

            List<Token> expanded = new ArrayList<Token>();
            for (int i = 0; i < base.size(); i++) {
                Token token = base.get(i);
                expanded.add(token);
                if (i + 1 < base.size() && needsImplicitMultiplication(token, base.get(i + 1))) {
                    expanded.add(new Token(TokenType.OPERATOR, "*"));
                }
            }
            expanded.add(new Token(TokenType.EOF, ""));
            return expanded;
        }

        private int consumeNumber(String source, int start) {
            int pos = start;
            boolean seenDot = false;
            while (pos < source.length()) {
                char c = source.charAt(pos);
                if (Character.isDigit(c)) {
                    pos++;
                    continue;
                }
                if (c == '.') {
                    if (seenDot) {
                        break;
                    }
                    seenDot = true;
                    pos++;
                    continue;
                }
                break;
            }

            if (pos < source.length()) {
                char c = source.charAt(pos);
                if (c == 'e' || c == 'E') {
                    int expPos = pos + 1;
                    if (expPos < source.length() && (source.charAt(expPos) == '+' || source.charAt(expPos) == '-')) {
                        expPos++;
                    }
                    int digitsStart = expPos;
                    while (expPos < source.length() && Character.isDigit(source.charAt(expPos))) {
                        expPos++;
                    }
                    if (digitsStart == expPos) {
                        throw error("Exponente invalido en numero");
                    }
                    pos = expPos;
                }
            }
            if (".".equals(source.substring(start, pos))) {
                throw error("Numero invalido: .");
            }
            return pos;
        }

        private boolean needsImplicitMultiplication(Token current, Token next) {
            boolean leftValue = current.type == TokenType.NUMBER
                    || current.type == TokenType.IDENTIFIER
                    || ")".equals(current.text)
                    || "!".equals(current.text);
            boolean rightValue = next.type == TokenType.NUMBER
                    || next.type == TokenType.IDENTIFIER
                    || "(".equals(next.text);
            if (!leftValue || !rightValue) {
                return false;
            }
            if (current.type == TokenType.IDENTIFIER && "(".equals(next.text)) {
                return false;
            }
            return true;
        }

        private boolean isOperatorChar(char c) {
            return "+-*/^(),!".indexOf(c) >= 0;
        }

        private boolean match(String... symbols) {
            for (String symbol : symbols) {
                if (check(symbol)) {
                    advance();
                    return true;
                }
            }
            return false;
        }

        private boolean match(TokenType type) {
            if (peek(type)) {
                advance();
                return true;
            }
            return false;
        }

        private void consume(String symbol, String message) {
            if (check(symbol)) {
                advance();
                return;
            }
            throw error(message);
        }

        private boolean check(String symbol) {
            return symbol.equals(current().text);
        }

        private boolean peek(TokenType type) {
            return current().type == type;
        }

        private Token advance() {
            if (!peek(TokenType.EOF)) {
                index++;
            }
            return previous();
        }

        private Token current() {
            return tokens.get(index);
        }

        private Token previous() {
            return tokens.get(index - 1);
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message);
        }
    }

    private enum TokenType {
        NUMBER,
        IDENTIFIER,
        OPERATOR,
        EOF
    }

    private static final class Token {
        private final TokenType type;
        private final String text;

        private Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }
    }
}
