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
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

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
import ch.obermuhlner.math.big.BigRational;

public class BigMathCalculatorApp {

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Keep default LAF.
            }
            new BigMathCalculatorApp().createAndShow();
        });
    }

    public static BigComplex evaluateInConsole(String expression, int precision, boolean realMode, BigComplex ans) {
        MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
        BigComplex result = new ExpressionEngine(mc, ans, Collections.<String, BigComplex>emptyMap()).evaluate(expression);
        if (realMode && result.im.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("El modo Real solo permite resultados con parte imaginaria 0.");
        }
        return result;
    }

    public static String formatForConsole(BigComplex value) {
        return formatComplex(value);
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
        JPanel panel = new JPanel(new GridLayout(10, 6, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Teclado"));

        String[] buttons = {
                "7", "8", "9", "/", "(", ")",
                "4", "5", "6", "*", "^", "!",
                "1", "2", "3", "-", "pi", "e",
                "0", ".", ",", "+", "i", "ans",
                "x", "y", "=", "sin(", "cos(", "tan(",
                "log(", "exp(", "sqrt(", "asin(", "acos(", "atan(",
                "sinh(", "cosh(", "tanh(", "pow(", "root(", "abs(",
                "arg(", "conj(", "re(", "im(", "gamma(", "factorial(",
                "cot(", "acot(", "log2(", "log10(", "deg(", "rad(",
                "asinh(", "acosh(", "atanh(", "coth(", "acoth(", "recip("
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
                "log2(1024)",
                "acosh(5)",
                "deg(pi/3)",
                "mantissa(12345.678)",
                "bernoulli(8)",
                "x^2-2",
                "sin(x)",
                "x^2-5x+6<=0",
                "modpow(3,100,7)",
                "fib(30)"
        });
        samples.addActionListener(e -> {
            String selected = (String) samples.getSelectedItem();
            if (selected != null && !"Ejemplos rapidos".equals(selected)) {
                expressionField.setText(selected);
                expressionField.requestFocus();
            }
        });
        panel.add(samples, BorderLayout.NORTH);

        JPanel advanced = new JPanel(new GridLayout(7, 2, 6, 6));
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

        JButton inequalityButton = new JButton("Resolver inecuacion");
        inequalityButton.addActionListener(e -> solveInequalityDialog());
        advanced.add(inequalityButton);

        JButton modularButton = new JButton("Modular / Discreta");
        modularButton.addActionListener(e -> solveModularDiscreteDialog());
        advanced.add(modularButton);

        JButton graphButton = new JButton("Grafos (ruta)");
        graphButton.addActionListener(e -> graphShortestPathDialog());
        advanced.add(graphButton);

        JButton sequenceButton = new JButton("Sucesiones");
        sequenceButton.addActionListener(e -> sequenceRecurrenceDialog());
        advanced.add(sequenceButton);

        JButton calculusButton = new JButton("Calculo numerico");
        calculusButton.addActionListener(e -> solveCalculusDialog());
        advanced.add(calculusButton);

        JButton matrixButton = new JButton("Matrices");
        matrixButton.addActionListener(e -> solveMatrixDialog());
        advanced.add(matrixButton);

        JButton statisticsButton = new JButton("Estadistica");
        statisticsButton.addActionListener(e -> solveStatisticsDialog());
        advanced.add(statisticsButton);

        JButton geometryButton = new JButton("Geometria");
        geometryButton.addActionListener(e -> solveGeometryDialog());
        advanced.add(geometryButton);

        JButton rationalButton = new JButton("Racionales");
        rationalButton.addActionListener(e -> solveRationalDialog());
        advanced.add(rationalButton);

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

    private void solveInequalityDialog() {
        String candidate = expressionField.getText().trim();
        if (candidate.isEmpty()) {
            candidate = "x^2-5*x+6<=0";
        }

        String ineq = JOptionPane.showInputDialog(null,
                "Inecuacion en x (ej: x^2-5*x+6<=0, sin(x)>0)",
                candidate);
        if (ineq == null || ineq.trim().isEmpty()) {
            return;
        }

        try {
            InequalitySolver solver = new InequalitySolver(currentMathContext(), lastResult);
            String intervals = solver.solve(ineq.trim(), -50.0, 50.0);
            resultArea.setText("inecuacion: " + ineq + "\nsolucion aproximada:\n" + intervals);
            historyModel.add(0, "ineq(" + ineq + ") = " + intervals.replace('\n', ' '));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error de inecuacion", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solveModularDiscreteDialog() {
        String cmd = JOptionPane.showInputDialog(null,
                "Comando discreto/modular\n"
                        + "gcd(a,b), lcm(a,b), modpow(a,b,m), modinv(a,m), phi(n), comb(n,k), perm(n,k), crt(a1,m1,a2,m2), fib(n)",
                "modpow(3,100,7)");

        if (cmd == null || cmd.trim().isEmpty()) {
            return;
        }

        try {
            String result = DiscreteMathEngine.evaluate(cmd.trim());
            resultArea.setText("comando: " + cmd + "\nresultado: " + result);
            historyModel.add(0, cmd + " = " + result);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error modular/discreta", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void graphShortestPathDialog() {
        String edges = JOptionPane.showInputDialog(null,
                "Aristas (no dirigido), formato A-B,B-C,C-D",
                "A-B,B-C,C-D,A-D,D-E");
        if (edges == null || edges.trim().isEmpty()) {
            return;
        }
        String start = JOptionPane.showInputDialog(null, "Nodo origen", "A");
        if (start == null || start.trim().isEmpty()) {
            return;
        }
        String end = JOptionPane.showInputDialog(null, "Nodo destino", "E");
        if (end == null || end.trim().isEmpty()) {
            return;
        }

        try {
            List<String> path = GraphTools.shortestPath(edges.trim(), start.trim(), end.trim());
            if (path.isEmpty()) {
                resultArea.setText("No hay camino entre " + start + " y " + end + ".");
                historyModel.add(0, "path(" + start + "," + end + ") = none");
                return;
            }
            resultArea.setText("camino minimo: " + path + "\nlongitud (aristas): " + (path.size() - 1));
            historyModel.add(0, "path(" + start + "," + end + ") = " + path);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error de grafos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sequenceRecurrenceDialog() {
        String formula = JOptionPane.showInputDialog(null,
                "Recurrencia de primer orden: a_{n+1} = f(n,a)\nUse variables n y a.",
                "a + n");
        if (formula == null || formula.trim().isEmpty()) {
            return;
        }

        Double a1 = askDouble("Valor inicial a1", 1.0);
        if (a1 == null) {
            return;
        }
        String countText = JOptionPane.showInputDialog(null, "Numero de terminos", "10");
        if (countText == null || countText.trim().isEmpty()) {
            return;
        }

        int nTerms;
        try {
            nTerms = Integer.parseInt(countText.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Numero de terminos invalido", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (nTerms < 1 || nTerms > 200) {
            JOptionPane.showMessageDialog(null, "Numero de terminos debe estar entre 1 y 200", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            List<BigComplex> seq = SequenceTools.generateRecurrence(formula.trim(), a1.doubleValue(), nTerms,
                    currentMathContext(), lastResult);
            StringBuilder sb = new StringBuilder();
            sb.append("a_{n+1} = ").append(formula).append('\n');
            for (int i = 0; i < seq.size(); i++) {
                sb.append("a").append(i + 1).append(" = ").append(formatComplex(seq.get(i))).append('\n');
            }
            resultArea.setText(sb.toString());
            historyModel.add(0, "seq(" + formula + ") terminos=" + nTerms);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error de sucesion", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solveCalculusDialog() {
        String cmd = JOptionPane.showInputDialog(null,
                "Comando de calculo/analisis\n"
                        + "deriv(expr,x0), deriv2(expr,x0), integral(expr,a,b[,n]), limit(expr,x0)",
                "deriv(sin(x),0)");
        if (cmd == null || cmd.trim().isEmpty()) {
            return;
        }

        try {
            String result = CalculusTools.evaluate(cmd.trim(), currentMathContext(), lastResult);
            resultArea.setText("comando: " + cmd + "\nresultado:\n" + result);
            historyModel.add(0, cmd + " = " + result.replace('\n', ' '));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error de calculo numerico",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solveMatrixDialog() {
        String cmd = JOptionPane.showInputDialog(null,
                "Comando matricial\n"
                        + "det([1,2;3,4]), inv([1,2;3,4]), mul([1,2;3,4],[5,6;7,8]), solve([2,1;5,7],[11;13])",
                "det([1,2;3,4])");
        if (cmd == null || cmd.trim().isEmpty()) {
            return;
        }

        try {
            String result = MatrixTools.evaluate(cmd.trim(), currentMathContext());
            resultArea.setText("comando: " + cmd + "\nresultado:\n" + result);
            historyModel.add(0, cmd + " = " + result.replace('\n', ' '));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error matricial", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solveStatisticsDialog() {
        String cmd = JOptionPane.showInputDialog(null,
                "Comando estadistico\n"
                        + "summary(1,2,3,4,5), linreg(1:2;2:5;3:10;4:17)",
                "summary(2,4,4,4,5,5,7,9)");
        if (cmd == null || cmd.trim().isEmpty()) {
            return;
        }

        try {
            String result = StatisticsTools.evaluate(cmd.trim(), currentMathContext());
            resultArea.setText("comando: " + cmd + "\nresultado:\n" + result);
            historyModel.add(0, cmd + " = " + result.replace('\n', ' '));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error estadistico", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solveGeometryDialog() {
        String cmd = JOptionPane.showInputDialog(null,
                "Comando geometrico\n"
                        + "distance2d(x1,y1,x2,y2), midpoint2d(x1,y1,x2,y2), circle(r), triangle(a,b,c), sphere(r)",
                "circle(3)");
        if (cmd == null || cmd.trim().isEmpty()) {
            return;
        }

        try {
            String result = GeometryTools.evaluate(cmd.trim(), currentMathContext());
            resultArea.setText("comando: " + cmd + "\nresultado:\n" + result);
            historyModel.add(0, cmd + " = " + result.replace('\n', ' '));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error geometrico", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void solveRationalDialog() {
        String cmd = JOptionPane.showInputDialog(null,
                "Comando racional exacto\n"
                        + "add(1/2,2/3), sub(7/5,1/10), mul(3/7,14/9), div(5/8,15/16), pow(3/2,5), reduce(12/18), bernoulli(10)",
                "add(1/2,2/3)");
        if (cmd == null || cmd.trim().isEmpty()) {
            return;
        }

        try {
            String result = RationalTools.evaluate(cmd.trim());
            resultArea.setText("comando: " + cmd + "\nresultado:\n" + result);
            historyModel.add(0, cmd + " = " + result.replace('\n', ' '));
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error racional", JOptionPane.ERROR_MESSAGE);
        }
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

    private static String[] splitTopLevelArgs(String argsRaw) {
        if (argsRaw.trim().isEmpty()) {
            return new String[0];
        }

        List<String> args = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < argsRaw.length(); i++) {
            char c = argsRaw.charAt(i);
            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
                if (depth < 0) {
                    throw new IllegalArgumentException("Parentesis o corchetes desbalanceados en argumentos");
                }
            } else if (c == ',' && depth == 0) {
                args.add(argsRaw.substring(start, i).trim());
                start = i + 1;
            }
        }

        if (depth != 0) {
            throw new IllegalArgumentException("Parentesis o corchetes desbalanceados en argumentos");
        }

        args.add(argsRaw.substring(start).trim());
        return args.toArray(new String[0]);
    }

    private void showHelp() {
        String help = "Operadores: +  -  *  /  ^  !\n"
                + "Variables: x, y\n"
                + "Multiplicacion implicita: 2pi, 3i, (1+i)(2-i), 3x\n"
                + "Constantes: pi, e, i, ans\n"
                + "Funciones 1 arg: sin cos tan cot asin acos atan acot sinh cosh tanh coth asinh acosh atanh acoth\n"
                + "Mas funciones: exp log log2 log10 sqrt gamma factorial abs abs2 arg conj re im recip mantissa exponent sigdigits int frac isint islong isdouble deg rad bernoulli\n"
                + "Funciones 2 args: pow(a,b), root(a,n), atan2(y,x)\n"
                + "Algebra: Expandir polinomio (ej: (x+2)^4)\n"
                + "Ecuaciones: Resolver f(x)=0\n"
                + "Inecuaciones: Resolver expresiones con <, <=, >, >=\n"
                + "Modular/discreta: gcd, lcm, modpow, modinv, phi, comb, perm, crt, fib, isprime, factor, divisors, sigma, mobius, lambda\n"
                + "Grafos: ruta mas corta en grafo no dirigido\n"
                + "Sucesiones: a_{n+1}=f(n,a)\n"
                + "Calculo numerico: deriv, deriv2, integral, limit\n"
                + "Matrices: det, inv, mul, solve, transpose\n"
                + "Estadistica: summary, linreg\n"
                + "Geometria: distance2d, midpoint2d, circle, triangle, sphere\n"
                + "Racionales: add, sub, mul, div, pow, reduce, bernoulli\n"
                + "Graficas: 2D (f(x)) y 3D (z=f(x,y))";
        JOptionPane.showMessageDialog(null, help, "Ayuda", JOptionPane.INFORMATION_MESSAGE);
    }

    private static BigComplex evaluateExpression(String expression, MathContext mc, BigComplex ans,
            Map<String, BigComplex> variables) {
        return new ExpressionEngine(mc, ans, variables).evaluate(expression);
    }

    private static BigDecimal evaluateRealExpression(String expression, MathContext mc, BigComplex ans,
            Map<String, BigComplex> variables, String context) {
        BigComplex value = evaluateExpression(expression, mc, ans, variables);
        if (value.im.abs().compareTo(BigDecimal.ONE.movePointLeft(Math.max(6, mc.getPrecision() / 2))) > 0) {
            throw new IllegalArgumentException(context + " requiere un resultado real");
        }
        return value.re;
    }

    private static BigDecimal parseDecimal(String text, MathContext mc) {
        return BigDecimalMath.toBigDecimal(text.trim(), mc);
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

    private static final class InequalitySolver {
        private final MathContext mc;
        private final BigComplex ans;

        private InequalitySolver(MathContext mc, BigComplex ans) {
            this.mc = mc;
            this.ans = ans;
        }

        private String solve(String inequality, double minX, double maxX) {
            ParsedInequality parsed = ParsedInequality.parse(inequality);
            RealFunction diff = x -> evaluateDifference(parsed.left, parsed.right, x);

            int steps = 1500;
            double step = (maxX - minX) / steps;

            List<Double> roots = new ArrayList<Double>();
            double prevX = minX;
            double prevY = diff.apply(prevX);
            for (int i = 1; i <= steps; i++) {
                double x = minX + i * step;
                double y = diff.apply(x);
                if (Double.isFinite(prevY) && Double.isFinite(y) && prevY * y < 0.0) {
                    roots.add(Double.valueOf(bisect(diff, prevX, x)));
                }
                prevX = x;
                prevY = y;
            }
            Collections.sort(roots);

            List<Double> bounds = new ArrayList<Double>();
            bounds.add(Double.valueOf(minX));
            for (Double r : roots) {
                if (bounds.isEmpty()
                        || Math.abs(bounds.get(bounds.size() - 1).doubleValue() - r.doubleValue()) > 1.0E-6) {
                    bounds.add(r);
                }
            }
            bounds.add(Double.valueOf(maxX));

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bounds.size() - 1; i++) {
                double a = bounds.get(i).doubleValue();
                double b = bounds.get(i + 1).doubleValue();
                double mid = (a + b) / 2.0;
                double val = diff.apply(mid);
                if (test(val, parsed.operator)) {
                    if (sb.length() > 0) {
                        sb.append(" U ");
                    }
                    boolean leftClosed = parsed.operator.contains("=") && i > 0;
                    boolean rightClosed = parsed.operator.contains("=") && i < bounds.size() - 2;
                    sb.append(leftClosed ? '[' : '(')
                            .append(String.format(Locale.ROOT, "%.6f", a))
                            .append(", ")
                            .append(String.format(Locale.ROOT, "%.6f", b))
                            .append(rightClosed ? ']' : ')');
                }
            }

            if (sb.length() == 0) {
                return "sin solucion en [" + minX + ", " + maxX + "]";
            }
            sb.append("\n(intervalos aproximados en [").append(minX).append(", ").append(maxX).append("])\n");
            sb.append("puntos frontera aprox: ").append(roots);
            return sb.toString();
        }

        private boolean test(double diff, String operator) {
            switch (operator) {
                case "<":
                    return diff < 0.0;
                case "<=":
                    return diff <= 0.0;
                case ">":
                    return diff > 0.0;
                case ">=":
                    return diff >= 0.0;
                default:
                    throw new IllegalArgumentException("Operador invalido: " + operator);
            }
        }

        private double evaluateDifference(String left, String right, double x) {
            Map<String, BigComplex> vars = new HashMap<String, BigComplex>();
            vars.put("x", BigComplex.valueOf(BigDecimal.valueOf(x)));
            ExpressionEngine engine = new ExpressionEngine(mc, ans, vars);
            BigComplex l = engine.evaluate(left);
            BigComplex r = engine.evaluate(right);
            BigComplex diff = l.subtract(r, mc);
            if (diff.im.abs().compareTo(BigDecimal.valueOf(1.0E-7)) > 0) {
                return Double.NaN;
            }
            return diff.re.doubleValue();
        }

        private double bisect(RealFunction f, double a, double b) {
            double fa = f.apply(a);
            for (int i = 0; i < 80; i++) {
                double m = (a + b) / 2.0;
                double fm = f.apply(m);
                if (Math.abs(fm) < 1.0E-12) {
                    return m;
                }
                if (fa * fm <= 0.0) {
                    b = m;
                } else {
                    a = m;
                    fa = fm;
                }
            }
            return (a + b) / 2.0;
        }
    }

    private static final class ParsedInequality {
        private final String left;
        private final String right;
        private final String operator;

        private ParsedInequality(String left, String right, String operator) {
            this.left = left;
            this.right = right;
            this.operator = operator;
        }

        private static ParsedInequality parse(String input) {
            String[] operators = new String[] { "<=", ">=", "<", ">" };
            for (String op : operators) {
                int idx = input.indexOf(op);
                if (idx > 0) {
                    String left = input.substring(0, idx).trim();
                    String right = input.substring(idx + op.length()).trim();
                    if (left.isEmpty() || right.isEmpty()) {
                        break;
                    }
                    return new ParsedInequality(left, right, op);
                }
            }
            throw new IllegalArgumentException("Formato esperado: expresion < expresion (o <=, >, >=)");
        }
    }

    private static final class DiscreteMathEngine {
        private static String evaluate(String input) {
            int open = input.indexOf('(');
            int close = input.lastIndexOf(')');
            if (open <= 0 || close <= open) {
                throw new IllegalArgumentException("Formato esperado: funcion(arg1,arg2,...)");
            }

            String name = input.substring(0, open).trim().toLowerCase(Locale.ROOT);
            String argsRaw = input.substring(open + 1, close).trim();
            String[] args = splitTopLevelArgs(argsRaw);

            switch (name) {
                case "gcd":
                    requireArgs(name, args, 2);
                    return parseBig(args[0]).gcd(parseBig(args[1])).toString();
                case "lcm":
                    requireArgs(name, args, 2);
                    BigInteger a = parseBig(args[0]).abs();
                    BigInteger b = parseBig(args[1]).abs();
                    if (a.equals(BigInteger.ZERO) || b.equals(BigInteger.ZERO)) {
                        return "0";
                    }
                    return a.divide(a.gcd(b)).multiply(b).toString();
                case "modpow":
                    requireArgs(name, args, 3);
                    return parseBig(args[0]).modPow(parseBig(args[1]), positiveModulus(args[2])).toString();
                case "modinv":
                    requireArgs(name, args, 2);
                    return parseBig(args[0]).modInverse(positiveModulus(args[1])).toString();
                case "phi":
                    requireArgs(name, args, 1);
                    return eulerPhi(parseBig(args[0])).toString();
                case "comb":
                    requireArgs(name, args, 2);
                    return nChooseK(parseInt(args[0]), parseInt(args[1])).toString();
                case "perm":
                    requireArgs(name, args, 2);
                    return permutations(parseInt(args[0]), parseInt(args[1])).toString();
                case "crt":
                    requireArgs(name, args, 4);
                    return chineseRemainder(parseBig(args[0]), parseBig(args[1]), parseBig(args[2]), parseBig(args[3]))
                            .toString();
                case "fib":
                    requireArgs(name, args, 1);
                    return fibonacci(parseInt(args[0])).toString();
                case "isprime":
                    requireArgs(name, args, 1);
                    return Boolean.toString(parseBig(args[0]).isProbablePrime(40));
                case "factor":
                    requireArgs(name, args, 1);
                    return factorization(parseBig(args[0]));
                case "divisors":
                    requireArgs(name, args, 1);
                    return divisors(parseBig(args[0])).toString();
                case "sigma":
                    requireArgs(name, args, 1);
                    return sigma(parseBig(args[0])).toString();
                case "mobius":
                    requireArgs(name, args, 1);
                    return Integer.toString(mobius(parseBig(args[0])));
                case "lambda":
                    requireArgs(name, args, 1);
                    return carmichaelLambda(parseBig(args[0])).toString();
                default:
                    throw new IllegalArgumentException("Funcion discreta/modular no soportada: " + name);
            }
        }

        private static void requireArgs(String name, String[] args, int expected) {
            if (args.length != expected) {
                throw new IllegalArgumentException(name + " espera " + expected + " argumentos");
            }
        }

        private static BigInteger parseBig(String value) {
            return new BigInteger(value.trim());
        }

        private static int parseInt(String value) {
            return Integer.parseInt(value.trim());
        }

        private static BigInteger positiveModulus(String value) {
            BigInteger m = parseBig(value);
            if (m.signum() <= 0) {
                throw new IllegalArgumentException("El modulo debe ser positivo");
            }
            return m;
        }

        private static BigInteger eulerPhi(BigInteger n) {
            if (n.signum() <= 0) {
                throw new IllegalArgumentException("phi(n) requiere n > 0");
            }
            BigInteger result = n;
            BigInteger x = n;
            for (BigInteger p = BigInteger.valueOf(2); p.multiply(p).compareTo(x) <= 0; p = p.add(BigInteger.ONE)) {
                if (x.mod(p).equals(BigInteger.ZERO)) {
                    while (x.mod(p).equals(BigInteger.ZERO)) {
                        x = x.divide(p);
                    }
                    result = result.subtract(result.divide(p));
                }
            }
            if (x.compareTo(BigInteger.ONE) > 0) {
                result = result.subtract(result.divide(x));
            }
            return result;
        }

        private static BigInteger nChooseK(int n, int k) {
            if (n < 0 || k < 0 || k > n) {
                return BigInteger.ZERO;
            }
            int kk = Math.min(k, n - k);
            BigInteger result = BigInteger.ONE;
            for (int i = 1; i <= kk; i++) {
                result = result.multiply(BigInteger.valueOf(n - kk + i)).divide(BigInteger.valueOf(i));
            }
            return result;
        }

        private static BigInteger permutations(int n, int k) {
            if (n < 0 || k < 0 || k > n) {
                return BigInteger.ZERO;
            }
            BigInteger result = BigInteger.ONE;
            for (int i = 0; i < k; i++) {
                result = result.multiply(BigInteger.valueOf(n - i));
            }
            return result;
        }

        private static BigInteger chineseRemainder(BigInteger a1, BigInteger m1, BigInteger a2, BigInteger m2) {
            if (!m1.gcd(m2).equals(BigInteger.ONE)) {
                throw new IllegalArgumentException("CRT de 2 ecuaciones requiere modulos coprimos");
            }
            BigInteger M = m1.multiply(m2);
            BigInteger M1 = M.divide(m1);
            BigInteger M2 = M.divide(m2);
            BigInteger inv1 = M1.modInverse(m1);
            BigInteger inv2 = M2.modInverse(m2);
            BigInteger x = a1.multiply(M1).multiply(inv1).add(a2.multiply(M2).multiply(inv2));
            return x.mod(M);
        }

        private static BigInteger fibonacci(int n) {
            if (n < 0) {
                throw new IllegalArgumentException("fib(n) requiere n >= 0");
            }
            BigInteger a = BigInteger.ZERO;
            BigInteger b = BigInteger.ONE;
            for (int i = 0; i < n; i++) {
                BigInteger next = a.add(b);
                a = b;
                b = next;
            }
            return a;
        }

        private static String factorization(BigInteger n) {
            if (n.equals(BigInteger.ZERO)) {
                return "0";
            }
            if (n.equals(BigInteger.ONE)) {
                return "1";
            }

            BigInteger value = n.abs();
            List<String> factors = new ArrayList<String>();
            if (n.signum() < 0) {
                factors.add("-1");
            }

            int exponentTwo = 0;
            BigInteger two = BigInteger.valueOf(2);
            while (value.mod(two).equals(BigInteger.ZERO)) {
                value = value.divide(two);
                exponentTwo++;
            }
            if (exponentTwo > 0) {
                factors.add(formatPrimeFactor(two, exponentTwo));
            }

            for (BigInteger p = BigInteger.valueOf(3); p.multiply(p).compareTo(value) <= 0; p = p.add(two)) {
                int exponent = 0;
                while (value.mod(p).equals(BigInteger.ZERO)) {
                    value = value.divide(p);
                    exponent++;
                }
                if (exponent > 0) {
                    factors.add(formatPrimeFactor(p, exponent));
                }
            }

            if (value.compareTo(BigInteger.ONE) > 0) {
                factors.add(value.toString());
            }
            return String.join(" * ", factors);
        }

        private static String formatPrimeFactor(BigInteger prime, int exponent) {
            return exponent == 1 ? prime.toString() : prime + "^" + exponent;
        }

        private static List<BigInteger> divisors(BigInteger n) {
            if (n.signum() == 0) {
                throw new IllegalArgumentException("divisors(n) requiere n != 0");
            }
            BigInteger value = n.abs();
            List<BigInteger> divisors = new ArrayList<BigInteger>();
            for (BigInteger d = BigInteger.ONE; d.multiply(d).compareTo(value) <= 0; d = d.add(BigInteger.ONE)) {
                if (value.mod(d).equals(BigInteger.ZERO)) {
                    divisors.add(d);
                    BigInteger other = value.divide(d);
                    if (!other.equals(d)) {
                        divisors.add(other);
                    }
                }
            }
            Collections.sort(divisors);
            return divisors;
        }

        private static BigInteger sigma(BigInteger n) {
            List<BigInteger> divisors = divisors(n);
            BigInteger sum = BigInteger.ZERO;
            for (BigInteger divisor : divisors) {
                sum = sum.add(divisor);
            }
            return sum;
        }

        private static int mobius(BigInteger n) {
            if (n.signum() <= 0) {
                throw new IllegalArgumentException("mobius(n) requiere n > 0");
            }
            if (n.equals(BigInteger.ONE)) {
                return 1;
            }

            BigInteger value = n;
            int distinctPrimes = 0;
            for (BigInteger p = BigInteger.valueOf(2); p.multiply(p).compareTo(value) <= 0; p = p.add(BigInteger.ONE)) {
                int exponent = 0;
                while (value.mod(p).equals(BigInteger.ZERO)) {
                    value = value.divide(p);
                    exponent++;
                    if (exponent > 1) {
                        return 0;
                    }
                }
                if (exponent == 1) {
                    distinctPrimes++;
                }
            }
            if (value.compareTo(BigInteger.ONE) > 0) {
                distinctPrimes++;
            }
            return distinctPrimes % 2 == 0 ? 1 : -1;
        }

        private static BigInteger carmichaelLambda(BigInteger n) {
            if (n.signum() <= 0) {
                throw new IllegalArgumentException("lambda(n) requiere n > 0");
            }
            BigInteger value = n;
            BigInteger result = BigInteger.ONE;
            for (BigInteger p = BigInteger.valueOf(2); p.multiply(p).compareTo(value) <= 0; p = p.add(BigInteger.ONE)) {
                int exponent = 0;
                while (value.mod(p).equals(BigInteger.ZERO)) {
                    value = value.divide(p);
                    exponent++;
                }
                if (exponent > 0) {
                    result = lcm(result, lambdaPrimePower(p, exponent));
                }
            }
            if (value.compareTo(BigInteger.ONE) > 0) {
                result = lcm(result, value.subtract(BigInteger.ONE));
            }
            return result;
        }

        private static BigInteger lambdaPrimePower(BigInteger prime, int exponent) {
            BigInteger two = BigInteger.valueOf(2);
            if (prime.equals(two)) {
                if (exponent == 1) {
                    return BigInteger.ONE;
                }
                if (exponent == 2) {
                    return two;
                }
                return two.pow(exponent - 2);
            }
            return prime.subtract(BigInteger.ONE).multiply(prime.pow(exponent - 1));
        }

        private static BigInteger lcm(BigInteger a, BigInteger b) {
            if (a.equals(BigInteger.ZERO) || b.equals(BigInteger.ZERO)) {
                return BigInteger.ZERO;
            }
            return a.divide(a.gcd(b)).multiply(b).abs();
        }
    }

    private static final class CalculusTools {
        private static String evaluate(String input, MathContext mc, BigComplex ans) {
            int open = input.indexOf('(');
            int close = input.lastIndexOf(')');
            if (open <= 0 || close <= open) {
                throw new IllegalArgumentException("Formato esperado: funcion(arg1,arg2,...)");
            }

            String name = input.substring(0, open).trim().toLowerCase(Locale.ROOT);
            String[] args = splitTopLevelArgs(input.substring(open + 1, close));
            switch (name) {
                case "deriv":
                    requireArgRange(name, args, 2, 2);
                    return formatDecimal(derivative(args[0], parseDecimal(args[1], mc), mc, ans));
                case "deriv2":
                    requireArgRange(name, args, 2, 2);
                    return formatDecimal(secondDerivative(args[0], parseDecimal(args[1], mc), mc, ans));
                case "integral":
                    requireArgRange(name, args, 3, 4);
                    int intervals = args.length == 4 ? Integer.parseInt(args[3].trim()) : 200;
                    return formatDecimal(integral(args[0], parseDecimal(args[1], mc), parseDecimal(args[2], mc), intervals, mc, ans));
                case "limit":
                    requireArgRange(name, args, 2, 2);
                    return formatDecimal(limit(args[0], parseDecimal(args[1], mc), mc, ans));
                default:
                    throw new IllegalArgumentException("Comando de calculo no soportado: " + name);
            }
        }

        private static void requireArgRange(String name, String[] args, int min, int max) {
            if (args.length < min || args.length > max) {
                throw new IllegalArgumentException(name + " espera entre " + min + " y " + max + " argumentos");
            }
        }

        private static BigDecimal derivative(String expression, BigDecimal x0, MathContext mc, BigComplex ans) {
            BigDecimal h = stepSize(mc);
            BigDecimal left = evaluateAt(expression, x0.subtract(h, mc), mc, ans);
            BigDecimal right = evaluateAt(expression, x0.add(h, mc), mc, ans);
            return right.subtract(left, mc).divide(h.multiply(BigDecimal.valueOf(2), mc), mc);
        }

        private static BigDecimal secondDerivative(String expression, BigDecimal x0, MathContext mc, BigComplex ans) {
            BigDecimal h = stepSize(mc);
            BigDecimal fx = evaluateAt(expression, x0, mc, ans);
            BigDecimal left = evaluateAt(expression, x0.subtract(h, mc), mc, ans);
            BigDecimal right = evaluateAt(expression, x0.add(h, mc), mc, ans);
            BigDecimal numerator = right.subtract(fx.multiply(BigDecimal.valueOf(2), mc), mc).add(left, mc);
            return numerator.divide(h.multiply(h, mc), mc);
        }

        private static BigDecimal integral(String expression, BigDecimal a, BigDecimal b, int intervals,
                MathContext mc, BigComplex ans) {
            if (intervals < 2 || intervals > 5000 || intervals % 2 != 0) {
                throw new IllegalArgumentException("integral(...,n) requiere n par entre 2 y 5000");
            }

            BigDecimal width = b.subtract(a, mc);
            BigDecimal h = width.divide(BigDecimal.valueOf(intervals), mc);
            BigDecimal sum = evaluateAt(expression, a, mc, ans).add(evaluateAt(expression, b, mc, ans), mc);
            for (int i = 1; i < intervals; i++) {
                BigDecimal x = a.add(h.multiply(BigDecimal.valueOf(i), mc), mc);
                BigDecimal fx = evaluateAt(expression, x, mc, ans);
                sum = sum.add(fx.multiply(BigDecimal.valueOf(i % 2 == 0 ? 2 : 4), mc), mc);
            }
            return sum.multiply(h, mc).divide(BigDecimal.valueOf(3), mc);
        }

        private static BigDecimal limit(String expression, BigDecimal x0, MathContext mc, BigComplex ans) {
            BigDecimal h = stepSize(mc);
            BigDecimal left = evaluateAt(expression, x0.subtract(h, mc), mc, ans);
            BigDecimal right = evaluateAt(expression, x0.add(h, mc), mc, ans);
            return left.add(right, mc).divide(BigDecimal.valueOf(2), mc);
        }

        private static BigDecimal evaluateAt(String expression, BigDecimal x, MathContext mc, BigComplex ans) {
            Map<String, BigComplex> vars = new HashMap<String, BigComplex>();
            vars.put("x", BigComplex.valueOf(x));
            return evaluateRealExpression(expression, mc, ans, vars, "La expresion");
        }

        private static BigDecimal stepSize(MathContext mc) {
            return BigDecimal.ONE.movePointLeft(Math.max(4, mc.getPrecision() / 2));
        }
    }

    private static final class MatrixTools {
        private static String evaluate(String input, MathContext mc) {
            int open = input.indexOf('(');
            int close = input.lastIndexOf(')');
            if (open <= 0 || close <= open) {
                throw new IllegalArgumentException("Formato esperado: funcion(arg1,arg2,...)");
            }

            String name = input.substring(0, open).trim().toLowerCase(Locale.ROOT);
            String[] args = splitTopLevelArgs(input.substring(open + 1, close));
            switch (name) {
                case "det":
                    requireArgs(name, args, 1);
                    return formatDecimal(determinant(parseMatrix(args[0], mc), mc));
                case "inv":
                    requireArgs(name, args, 1);
                    return formatMatrix(inverse(parseMatrix(args[0], mc), mc));
                case "mul":
                    requireArgs(name, args, 2);
                    return formatMatrix(multiply(parseMatrix(args[0], mc), parseMatrix(args[1], mc), mc));
                case "solve":
                    requireArgs(name, args, 2);
                    return formatMatrix(solve(parseMatrix(args[0], mc), parseMatrix(args[1], mc), mc));
                case "transpose":
                    requireArgs(name, args, 1);
                    return formatMatrix(transpose(parseMatrix(args[0], mc)));
                default:
                    throw new IllegalArgumentException("Comando matricial no soportado: " + name);
            }
        }

        private static void requireArgs(String name, String[] args, int expected) {
            if (args.length != expected) {
                throw new IllegalArgumentException(name + " espera " + expected + " argumentos");
            }
        }

        private static BigDecimal[][] parseMatrix(String raw, MathContext mc) {
            String text = raw.trim();
            if (!text.startsWith("[") || !text.endsWith("]")) {
                throw new IllegalArgumentException("Matriz invalida. Use formato [1,2;3,4]");
            }

            String[] rows = text.substring(1, text.length() - 1).split(";");
            BigDecimal[][] matrix = null;
            for (int i = 0; i < rows.length; i++) {
                String rowText = rows[i].trim();
                if (rowText.isEmpty()) {
                    throw new IllegalArgumentException("Fila vacia en matriz");
                }
                String[] cols = rowText.split(",");
                if (matrix == null) {
                    matrix = new BigDecimal[rows.length][cols.length];
                } else if (cols.length != matrix[0].length) {
                    throw new IllegalArgumentException("Todas las filas deben tener el mismo numero de columnas");
                }
                for (int j = 0; j < cols.length; j++) {
                    matrix[i][j] = parseDecimal(cols[j], mc);
                }
            }
            return matrix;
        }

        private static BigDecimal[][] multiply(BigDecimal[][] left, BigDecimal[][] right, MathContext mc) {
            if (left[0].length != right.length) {
                throw new IllegalArgumentException("Dimensiones incompatibles para multiplicacion");
            }
            BigDecimal[][] result = new BigDecimal[left.length][right[0].length];
            for (int i = 0; i < left.length; i++) {
                for (int j = 0; j < right[0].length; j++) {
                    BigDecimal sum = BigDecimal.ZERO;
                    for (int k = 0; k < left[0].length; k++) {
                        sum = sum.add(left[i][k].multiply(right[k][j], mc), mc);
                    }
                    result[i][j] = sum;
                }
            }
            return result;
        }

        private static BigDecimal[][] transpose(BigDecimal[][] matrix) {
            BigDecimal[][] result = new BigDecimal[matrix[0].length][matrix.length];
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    result[j][i] = matrix[i][j];
                }
            }
            return result;
        }

        private static BigDecimal determinant(BigDecimal[][] matrix, MathContext mc) {
            if (matrix.length != matrix[0].length) {
                throw new IllegalArgumentException("det requiere una matriz cuadrada");
            }
            BigDecimal[][] work = copyMatrix(matrix);
            BigDecimal det = BigDecimal.ONE;
            int sign = 1;
            for (int col = 0; col < work.length; col++) {
                int pivot = findPivot(work, col, col);
                if (pivot == -1) {
                    return BigDecimal.ZERO;
                }
                if (pivot != col) {
                    BigDecimal[] tmp = work[pivot];
                    work[pivot] = work[col];
                    work[col] = tmp;
                    sign *= -1;
                }
                BigDecimal pivotValue = work[col][col];
                det = det.multiply(pivotValue, mc);
                for (int row = col + 1; row < work.length; row++) {
                    BigDecimal factor = work[row][col].divide(pivotValue, mc);
                    for (int c = col; c < work.length; c++) {
                        work[row][c] = work[row][c].subtract(factor.multiply(work[col][c], mc), mc);
                    }
                }
            }
            return sign < 0 ? det.negate() : det;
        }

        private static BigDecimal[][] inverse(BigDecimal[][] matrix, MathContext mc) {
            if (matrix.length != matrix[0].length) {
                throw new IllegalArgumentException("inv requiere una matriz cuadrada");
            }

            int n = matrix.length;
            BigDecimal[][] work = new BigDecimal[n][2 * n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    work[i][j] = matrix[i][j];
                }
                for (int j = 0; j < n; j++) {
                    work[i][n + j] = i == j ? BigDecimal.ONE : BigDecimal.ZERO;
                }
            }

            gaussJordan(work, n, mc);

            BigDecimal[][] inverse = new BigDecimal[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    inverse[i][j] = work[i][n + j];
                }
            }
            return inverse;
        }

        private static BigDecimal[][] solve(BigDecimal[][] a, BigDecimal[][] b, MathContext mc) {
            if (a.length != a[0].length) {
                throw new IllegalArgumentException("La matriz A debe ser cuadrada");
            }
            if (b.length != a.length) {
                throw new IllegalArgumentException("La matriz/vector B debe tener tantas filas como A");
            }
            return multiply(inverse(a, mc), b, mc);
        }

        private static void gaussJordan(BigDecimal[][] work, int n, MathContext mc) {
            for (int col = 0; col < n; col++) {
                int pivot = findPivot(work, col, col);
                if (pivot == -1) {
                    throw new IllegalArgumentException("La matriz es singular");
                }
                if (pivot != col) {
                    BigDecimal[] tmp = work[pivot];
                    work[pivot] = work[col];
                    work[col] = tmp;
                }

                BigDecimal pivotValue = work[col][col];
                for (int j = 0; j < work[col].length; j++) {
                    work[col][j] = work[col][j].divide(pivotValue, mc);
                }

                for (int row = 0; row < n; row++) {
                    if (row == col) {
                        continue;
                    }
                    BigDecimal factor = work[row][col];
                    for (int j = 0; j < work[row].length; j++) {
                        work[row][j] = work[row][j].subtract(factor.multiply(work[col][j], mc), mc);
                    }
                }
            }
        }

        private static int findPivot(BigDecimal[][] matrix, int column, int startRow) {
            for (int row = startRow; row < matrix.length; row++) {
                if (matrix[row][column].compareTo(BigDecimal.ZERO) != 0) {
                    return row;
                }
            }
            return -1;
        }

        private static BigDecimal[][] copyMatrix(BigDecimal[][] matrix) {
            BigDecimal[][] copy = new BigDecimal[matrix.length][matrix[0].length];
            for (int i = 0; i < matrix.length; i++) {
                System.arraycopy(matrix[i], 0, copy[i], 0, matrix[i].length);
            }
            return copy;
        }

        private static String formatMatrix(BigDecimal[][] matrix) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < matrix.length; i++) {
                sb.append("[");
                for (int j = 0; j < matrix[i].length; j++) {
                    if (j > 0) {
                        sb.append(", ");
                    }
                    sb.append(formatDecimal(matrix[i][j]));
                }
                sb.append("]");
                if (i + 1 < matrix.length) {
                    sb.append('\n');
                }
            }
            return sb.toString();
        }
    }

    private static final class StatisticsTools {
        private static String evaluate(String input, MathContext mc) {
            int open = input.indexOf('(');
            int close = input.lastIndexOf(')');
            if (open <= 0 || close <= open) {
                throw new IllegalArgumentException("Formato esperado: funcion(arg1,arg2,...)");
            }

            String name = input.substring(0, open).trim().toLowerCase(Locale.ROOT);
            String inside = input.substring(open + 1, close).trim();
            switch (name) {
                case "summary":
                    return summary(splitTopLevelArgs(inside), mc);
                case "linreg":
                    return linearRegression(inside, mc);
                default:
                    throw new IllegalArgumentException("Comando estadistico no soportado: " + name);
            }
        }

        private static String summary(String[] values, MathContext mc) {
            if (values.length == 0) {
                throw new IllegalArgumentException("summary requiere al menos un valor");
            }

            List<BigDecimal> data = new ArrayList<BigDecimal>();
            for (String value : values) {
                data.add(parseDecimal(value, mc));
            }
            Collections.sort(data);

            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal min = data.get(0);
            BigDecimal max = data.get(data.size() - 1);
            for (BigDecimal value : data) {
                sum = sum.add(value, mc);
            }
            BigDecimal mean = sum.divide(BigDecimal.valueOf(data.size()), mc);
            BigDecimal median = data.size() % 2 == 1
                    ? data.get(data.size() / 2)
                    : data.get(data.size() / 2 - 1).add(data.get(data.size() / 2), mc)
                            .divide(BigDecimal.valueOf(2), mc);

            BigDecimal variance = BigDecimal.ZERO;
            for (BigDecimal value : data) {
                BigDecimal delta = value.subtract(mean, mc);
                variance = variance.add(delta.multiply(delta, mc), mc);
            }
            variance = variance.divide(BigDecimal.valueOf(data.size()), mc);
            BigDecimal stddev = BigDecimalMath.sqrt(variance, mc);

            return "count = " + data.size() + "\n"
                    + "min = " + formatDecimal(min) + "\n"
                    + "max = " + formatDecimal(max) + "\n"
                    + "mean = " + formatDecimal(mean) + "\n"
                    + "median = " + formatDecimal(median) + "\n"
                    + "variance = " + formatDecimal(variance) + "\n"
                    + "stddev = " + formatDecimal(stddev);
        }

        private static String linearRegression(String raw, MathContext mc) {
            String[] pairs = raw.split(";");
            if (pairs.length < 2) {
                throw new IllegalArgumentException("linreg requiere al menos 2 puntos x:y");
            }

            BigDecimal sumX = BigDecimal.ZERO;
            BigDecimal sumY = BigDecimal.ZERO;
            BigDecimal sumXX = BigDecimal.ZERO;
            BigDecimal sumXY = BigDecimal.ZERO;
            for (String pair : pairs) {
                String[] xy = pair.trim().split(":");
                if (xy.length != 2) {
                    throw new IllegalArgumentException("Cada punto debe tener formato x:y");
                }
                BigDecimal x = parseDecimal(xy[0], mc);
                BigDecimal y = parseDecimal(xy[1], mc);
                sumX = sumX.add(x, mc);
                sumY = sumY.add(y, mc);
                sumXX = sumXX.add(x.multiply(x, mc), mc);
                sumXY = sumXY.add(x.multiply(y, mc), mc);
            }

            BigDecimal n = BigDecimal.valueOf(pairs.length);
            BigDecimal denominator = n.multiply(sumXX, mc).subtract(sumX.multiply(sumX, mc), mc);
            if (denominator.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Los puntos producen una regresion degenerada");
            }
            BigDecimal slope = n.multiply(sumXY, mc).subtract(sumX.multiply(sumY, mc), mc).divide(denominator, mc);
            BigDecimal intercept = sumY.subtract(slope.multiply(sumX, mc), mc).divide(n, mc);
            return "slope = " + formatDecimal(slope) + "\n"
                    + "intercept = " + formatDecimal(intercept) + "\n"
                    + "model = y = " + formatDecimal(slope) + " * x + " + formatDecimal(intercept);
        }
    }

    private static final class GeometryTools {
        private static String evaluate(String input, MathContext mc) {
            int open = input.indexOf('(');
            int close = input.lastIndexOf(')');
            if (open <= 0 || close <= open) {
                throw new IllegalArgumentException("Formato esperado: funcion(arg1,arg2,...)");
            }

            String name = input.substring(0, open).trim().toLowerCase(Locale.ROOT);
            String[] args = splitTopLevelArgs(input.substring(open + 1, close));
            switch (name) {
                case "distance2d":
                    requireArgs(name, args, 4);
                    return formatDecimal(distance2d(args, mc));
                case "midpoint2d":
                    requireArgs(name, args, 4);
                    return midpoint2d(args, mc);
                case "circle":
                    requireArgs(name, args, 1);
                    return circle(parseDecimal(args[0], mc), mc);
                case "triangle":
                    requireArgs(name, args, 3);
                    return triangle(args, mc);
                case "sphere":
                    requireArgs(name, args, 1);
                    return sphere(parseDecimal(args[0], mc), mc);
                default:
                    throw new IllegalArgumentException("Comando geometrico no soportado: " + name);
            }
        }

        private static void requireArgs(String name, String[] args, int expected) {
            if (args.length != expected) {
                throw new IllegalArgumentException(name + " espera " + expected + " argumentos");
            }
        }

        private static BigDecimal distance2d(String[] args, MathContext mc) {
            BigDecimal x1 = parseDecimal(args[0], mc);
            BigDecimal y1 = parseDecimal(args[1], mc);
            BigDecimal x2 = parseDecimal(args[2], mc);
            BigDecimal y2 = parseDecimal(args[3], mc);
            BigDecimal dx = x2.subtract(x1, mc);
            BigDecimal dy = y2.subtract(y1, mc);
            return BigDecimalMath.sqrt(dx.multiply(dx, mc).add(dy.multiply(dy, mc), mc), mc);
        }

        private static String midpoint2d(String[] args, MathContext mc) {
            BigDecimal x1 = parseDecimal(args[0], mc);
            BigDecimal y1 = parseDecimal(args[1], mc);
            BigDecimal x2 = parseDecimal(args[2], mc);
            BigDecimal y2 = parseDecimal(args[3], mc);
            BigDecimal mx = x1.add(x2, mc).divide(BigDecimal.valueOf(2), mc);
            BigDecimal my = y1.add(y2, mc).divide(BigDecimal.valueOf(2), mc);
            return "(" + formatDecimal(mx) + ", " + formatDecimal(my) + ")";
        }

        private static String circle(BigDecimal radius, MathContext mc) {
            BigDecimal pi = BigDecimalMath.pi(mc);
            BigDecimal area = pi.multiply(radius.multiply(radius, mc), mc);
            BigDecimal circumference = pi.multiply(radius, mc).multiply(BigDecimal.valueOf(2), mc);
            return "area = " + formatDecimal(area) + "\n"
                    + "circumference = " + formatDecimal(circumference);
        }

        private static String triangle(String[] args, MathContext mc) {
            BigDecimal a = parseDecimal(args[0], mc);
            BigDecimal b = parseDecimal(args[1], mc);
            BigDecimal c = parseDecimal(args[2], mc);
            BigDecimal semiperimeter = a.add(b, mc).add(c, mc).divide(BigDecimal.valueOf(2), mc);
            BigDecimal radicand = semiperimeter.multiply(semiperimeter.subtract(a, mc), mc)
                    .multiply(semiperimeter.subtract(b, mc), mc)
                    .multiply(semiperimeter.subtract(c, mc), mc);
            if (radicand.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Los lados no forman un triangulo valido");
            }
            BigDecimal area = BigDecimalMath.sqrt(radicand, mc);
            return "perimeter = " + formatDecimal(a.add(b, mc).add(c, mc)) + "\n"
                    + "area = " + formatDecimal(area);
        }

        private static String sphere(BigDecimal radius, MathContext mc) {
            BigDecimal pi = BigDecimalMath.pi(mc);
            BigDecimal r2 = radius.multiply(radius, mc);
            BigDecimal r3 = r2.multiply(radius, mc);
            BigDecimal area = BigDecimal.valueOf(4).multiply(pi, mc).multiply(r2, mc);
            BigDecimal volume = BigDecimal.valueOf(4).multiply(pi, mc).multiply(r3, mc)
                    .divide(BigDecimal.valueOf(3), mc);
            return "surface = " + formatDecimal(area) + "\n"
                    + "volume = " + formatDecimal(volume);
        }
    }

    private static final class RationalTools {
        private static String evaluate(String input) {
            int open = input.indexOf('(');
            int close = input.lastIndexOf(')');
            if (open <= 0 || close <= open) {
                throw new IllegalArgumentException("Formato esperado: funcion(arg1,arg2,...)");
            }

            String name = input.substring(0, open).trim().toLowerCase(Locale.ROOT);
            String[] args = splitTopLevelArgs(input.substring(open + 1, close));
            switch (name) {
                case "add":
                    requireArgs(name, args, 2);
                    return formatRational(parseRational(args[0]).add(parseRational(args[1])));
                case "sub":
                    requireArgs(name, args, 2);
                    return formatRational(parseRational(args[0]).subtract(parseRational(args[1])));
                case "mul":
                    requireArgs(name, args, 2);
                    return formatRational(parseRational(args[0]).multiply(parseRational(args[1])));
                case "div":
                    requireArgs(name, args, 2);
                    return formatRational(parseRational(args[0]).divide(parseRational(args[1])));
                case "pow":
                    requireArgs(name, args, 2);
                    return formatRational(parseRational(args[0]).pow(Integer.parseInt(args[1].trim())));
                case "reduce":
                    requireArgs(name, args, 1);
                    return formatRational(parseRational(args[0]).reduce());
                case "bernoulli":
                    requireArgs(name, args, 1);
                    return formatRational(BigRational.bernoulli(Integer.parseInt(args[0].trim())));
                default:
                    throw new IllegalArgumentException("Comando racional no soportado: " + name);
            }
        }

        private static void requireArgs(String name, String[] args, int expected) {
            if (args.length != expected) {
                throw new IllegalArgumentException(name + " espera " + expected + " argumentos");
            }
        }

        private static BigRational parseRational(String raw) {
            return BigRational.valueOf(raw.trim());
        }

        private static String formatRational(BigRational value) {
            return value.toRationalString() + "\n"
                    + "decimal = " + value.toBigDecimal(new MathContext(50, RoundingMode.HALF_UP)).toPlainString();
        }
    }

    private static final class GraphTools {
        private static List<String> shortestPath(String edgeList, String source, String target) {
            Map<String, List<String>> adj = new HashMap<String, List<String>>();
            String[] edges = edgeList.split(",");
            for (String edge : edges) {
                String part = edge.trim();
                if (part.isEmpty()) {
                    continue;
                }
                String[] nodes = part.split("-");
                if (nodes.length != 2) {
                    throw new IllegalArgumentException("Arista invalida: " + part + " (use A-B)");
                }
                String a = nodes[0].trim();
                String b = nodes[1].trim();
                if (a.isEmpty() || b.isEmpty()) {
                    throw new IllegalArgumentException("Arista invalida: " + part);
                }
                addEdge(adj, a, b);
                addEdge(adj, b, a);
            }

            if (!adj.containsKey(source) || !adj.containsKey(target)) {
                throw new IllegalArgumentException("El nodo origen o destino no existe en el grafo");
            }

            Map<String, String> prev = new HashMap<String, String>();
            Set<String> visited = new HashSet<String>();
            ArrayDeque<String> queue = new ArrayDeque<String>();
            queue.add(source);
            visited.add(source);

            while (!queue.isEmpty()) {
                String node = queue.removeFirst();
                if (node.equals(target)) {
                    break;
                }
                for (String neighbor : adj.get(node)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        prev.put(neighbor, node);
                        queue.addLast(neighbor);
                    }
                }
            }

            if (!visited.contains(target)) {
                return Collections.emptyList();
            }

            List<String> path = new ArrayList<String>();
            String cursor = target;
            while (cursor != null) {
                path.add(cursor);
                cursor = prev.get(cursor);
            }
            Collections.reverse(path);
            return path;
        }

        private static void addEdge(Map<String, List<String>> adj, String from, String to) {
            List<String> list = adj.get(from);
            if (list == null) {
                list = new ArrayList<String>();
                adj.put(from, list);
            }
            if (!list.contains(to)) {
                list.add(to);
            }
        }
    }

    private static final class SequenceTools {
        private static List<BigComplex> generateRecurrence(String formula, double firstValue, int terms, MathContext mc,
                BigComplex ans) {
            List<BigComplex> sequence = new ArrayList<BigComplex>();
            BigComplex current = BigComplex.valueOf(BigDecimal.valueOf(firstValue));
            sequence.add(current);

            for (int i = 1; i < terms; i++) {
                Map<String, BigComplex> vars = new HashMap<String, BigComplex>();
                vars.put("n", BigComplex.valueOf(BigDecimal.valueOf(i)));
                vars.put("a", current);
                ExpressionEngine engine = new ExpressionEngine(mc, ans, vars);
                current = engine.evaluate(formula);
                sequence.add(current);
            }

            return sequence;
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
                case "cot":
                    checkArgCount(name, args, 1);
                    return cot(args.get(0));
                case "asin":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.asin(args.get(0), mc);
                case "acos":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.acos(args.get(0), mc);
                case "atan":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.atan(args.get(0), mc);
                case "acot":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.acot(args.get(0), mc);
                case "sinh":
                    checkArgCount(name, args, 1);
                    return sinh(args.get(0));
                case "cosh":
                    checkArgCount(name, args, 1);
                    return cosh(args.get(0));
                case "tanh":
                    checkArgCount(name, args, 1);
                    return tanh(args.get(0));
                case "coth":
                    checkArgCount(name, args, 1);
                    return coth(args.get(0));
                case "asinh":
                    checkArgCount(name, args, 1);
                    return asinh(args.get(0));
                case "acosh":
                    checkArgCount(name, args, 1);
                    return acosh(args.get(0));
                case "atanh":
                    checkArgCount(name, args, 1);
                    return atanh(args.get(0));
                case "acoth":
                    checkArgCount(name, args, 1);
                    return acoth(args.get(0));
                case "exp":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.exp(args.get(0), mc);
                case "log":
                case "ln":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.log(args.get(0), mc);
                case "log2":
                    checkArgCount(name, args, 1);
                    return logBase(args.get(0), BigComplex.valueOf(BigDecimal.valueOf(2)));
                case "log10":
                    checkArgCount(name, args, 1);
                    return logBase(args.get(0), BigComplex.valueOf(BigDecimal.TEN));
                case "sqrt":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.sqrt(args.get(0), mc);
                case "gamma":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.gamma(args.get(0), mc);
                case "factorial":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.factorial(args.get(0), mc);
                case "bernoulli":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigDecimalMath.bernoulli(requireInteger(args.get(0), name), mc));
                case "abs":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigComplexMath.abs(args.get(0), mc));
                case "abs2":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigComplexMath.absSquare(args.get(0), mc));
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
                case "recip":
                    checkArgCount(name, args, 1);
                    return BigComplexMath.reciprocal(args.get(0), mc);
                case "mantissa":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigDecimalMath.mantissa(requireReal(args.get(0), name)));
                case "exponent":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigDecimal.valueOf(BigDecimalMath.exponent(requireReal(args.get(0), name))));
                case "sigdigits":
                    checkArgCount(name, args, 1);
                    return BigComplex
                            .valueOf(BigDecimal.valueOf(BigDecimalMath.significantDigits(requireReal(args.get(0), name))));
                case "int":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigDecimalMath.integralPart(requireReal(args.get(0), name)));
                case "frac":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigDecimalMath.fractionalPart(requireReal(args.get(0), name)));
                case "isint":
                    checkArgCount(name, args, 1);
                    return booleanResult(BigDecimalMath.isIntValue(requireReal(args.get(0), name)));
                case "islong":
                    checkArgCount(name, args, 1);
                    return booleanResult(BigDecimalMath.isLongValue(requireReal(args.get(0), name)));
                case "isdouble":
                    checkArgCount(name, args, 1);
                    return booleanResult(BigDecimalMath.isDoubleValue(requireReal(args.get(0), name)));
                case "deg":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigDecimalMath.toDegrees(requireReal(args.get(0), name), mc));
                case "rad":
                    checkArgCount(name, args, 1);
                    return BigComplex.valueOf(BigDecimalMath.toRadians(requireReal(args.get(0), name), mc));
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

        private BigComplex coth(BigComplex x) {
            return cosh(x).divide(sinh(x), mc);
        }

        private BigComplex asinh(BigComplex x) {
            BigComplex radicand = x.multiply(x, mc).add(BigComplex.ONE, mc);
            return BigComplexMath.log(x.add(BigComplexMath.sqrt(radicand, mc), mc), mc);
        }

        private BigComplex acosh(BigComplex x) {
            BigComplex radicand = x.multiply(x, mc).subtract(BigComplex.ONE, mc);
            return BigComplexMath.log(x.add(BigComplexMath.sqrt(radicand, mc), mc), mc);
        }

        private BigComplex atanh(BigComplex x) {
            BigComplex numerator = BigComplex.ONE.add(x, mc);
            BigComplex denominator = BigComplex.ONE.subtract(x, mc);
            return BigComplexMath.log(numerator.divide(denominator, mc), mc).divide(BigDecimal.valueOf(2), mc);
        }

        private BigComplex acoth(BigComplex x) {
            BigComplex numerator = x.add(BigComplex.ONE, mc);
            BigComplex denominator = x.subtract(BigComplex.ONE, mc);
            return BigComplexMath.log(numerator.divide(denominator, mc), mc).divide(BigDecimal.valueOf(2), mc);
        }

        private BigComplex cot(BigComplex x) {
            return BigComplexMath.cos(x, mc).divide(BigComplexMath.sin(x, mc), mc);
        }

        private BigComplex logBase(BigComplex value, BigComplex base) {
            return BigComplexMath.log(value, mc).divide(BigComplexMath.log(base, mc), mc);
        }

        private BigComplex booleanResult(boolean value) {
            return BigComplex.valueOf(value ? BigDecimal.ONE : BigDecimal.ZERO);
        }

        private BigDecimal requireReal(BigComplex value, String functionName) {
            if (value.im.compareTo(BigDecimal.ZERO) != 0) {
                throw error(functionName + " requiere argumentos reales");
            }
            return value.re;
        }

        private int requireInteger(BigComplex value, String functionName) {
            BigDecimal real = requireReal(value, functionName);
            if (!BigDecimalMath.isIntValue(real)) {
                throw error(functionName + " requiere un entero");
            }
            return real.intValueExact();
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
