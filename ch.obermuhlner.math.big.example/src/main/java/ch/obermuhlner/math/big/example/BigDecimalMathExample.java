package ch.obermuhlner.math.big.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

import ch.obermuhlner.math.big.BigComplex;
import ch.obermuhlner.math.big.BigDecimalMath;

public class BigDecimalMathExample {

	private static final int DEFAULT_PRECISION = 50;

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			runArgumentMode(args);
			return;
		}

		runInteractiveConsole();
	}

	private static void runArgumentMode(String[] args) throws IOException {
		String command = args[0].trim().toLowerCase(Locale.ROOT);
		if ("gui".equals(command)) {
			launchGui();
			return;
		}
		if ("demo".equals(command) || "docu".equals(command)) {
			runDemo();
			return;
		}
		if ("calc".equals(command) || "console".equals(command)) {
			runCalculatorMode(new BufferedReader(new InputStreamReader(System.in)), true);
			return;
		}

		System.out.println("Argumento no reconocido: " + args[0]);
		printMainHelp();
	}

	private static void runInteractiveConsole() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Big-Math console");
		System.out.println("Escribe `gui` en cualquier momento para abrir la interfaz grafica.");
		printMainHelp();

		while (true) {
			System.out.print("\nmodo> ");
			String line = reader.readLine();
			if (line == null) {
				System.out.println();
				return;
			}

			String command = line.trim();
			if (command.isEmpty()) {
				continue;
			}
			if (handleGlobalCommand(command)) {
				if (isExitCommand(command)) {
					return;
				}
				continue;
			}

			String normalized = command.toLowerCase(Locale.ROOT);
			if ("1".equals(normalized) || "calc".equals(normalized) || "calculadora".equals(normalized)) {
				runCalculatorMode(reader, false);
				continue;
			}
			if ("2".equals(normalized) || "demo".equals(normalized) || "ejemplos".equals(normalized)) {
				runDemo();
				continue;
			}

			System.out.println("Comando no reconocido: " + command);
			printMainHelp();
		}
	}

	private static void runCalculatorMode(BufferedReader reader, boolean standalone) throws IOException {
		BigComplex lastResult = BigComplex.ZERO;
		int precision = DEFAULT_PRECISION;
		boolean realMode = false;

		System.out.println("\nCalculadora de consola");
		System.out.println("Expresiones soportadas como en la GUI. Comandos: `gui`, `back`, `exit`, `help`, `precision 80`, `mode real`, `mode complex`.");

		while (true) {
			System.out.print("calc[" + (realMode ? "real" : "complex") + "," + precision + "]> ");
			String line = reader.readLine();
			if (line == null) {
				System.out.println();
				return;
			}

			String command = line.trim();
			if (command.isEmpty()) {
				continue;
			}
			if (handleGlobalCommand(command)) {
				if (isExitCommand(command)) {
					return;
				}
				continue;
			}

			String normalized = command.toLowerCase(Locale.ROOT);
			if ("back".equals(normalized)) {
				if (standalone) {
					return;
				}
				break;
			}
			if ("help".equals(normalized) || "ayuda".equals(normalized)) {
				printCalculatorHelp();
				continue;
			}
			if ("ans".equalsIgnoreCase(command)) {
				System.out.println("ans = " + BigMathCalculatorApp.formatForConsole(lastResult));
				continue;
			}
			if (normalized.startsWith("precision ")) {
				Integer parsedPrecision = parsePrecision(command.substring("precision ".length()).trim());
				if (parsedPrecision != null) {
					precision = parsedPrecision.intValue();
					System.out.println("Precision actualizada a " + precision + " digitos.");
				}
				continue;
			}
			if ("mode real".equals(normalized) || "modo real".equals(normalized)) {
				realMode = true;
				System.out.println("Modo cambiado a real.");
				continue;
			}
			if ("mode complex".equals(normalized) || "modo complejo".equals(normalized) || "mode complejo".equals(normalized)) {
				realMode = false;
				System.out.println("Modo cambiado a complejo.");
				continue;
			}

			try {
				BigComplex result = BigMathCalculatorApp.evaluateInConsole(command, precision, realMode, lastResult);
				lastResult = result;
				System.out.println(BigMathCalculatorApp.formatForConsole(result));
			} catch (RuntimeException ex) {
				System.out.println("Error: " + ex.getMessage());
			}
		}
	}

	private static boolean handleGlobalCommand(String command) {
		String normalized = command.trim().toLowerCase(Locale.ROOT);
		if ("gui".equals(normalized)) {
			launchGui();
			return true;
		}
		if ("help".equals(normalized) || "ayuda".equals(normalized)) {
			printMainHelp();
			return true;
		}
		if (isExitCommand(normalized)) {
			System.out.println("Cerrando consola.");
			return true;
		}
		return false;
	}

	private static boolean isExitCommand(String command) {
		String normalized = command.trim().toLowerCase(Locale.ROOT);
		return "exit".equals(normalized) || "quit".equals(normalized) || "salir".equals(normalized);
	}

	private static Integer parsePrecision(String raw) {
		try {
			int value = Integer.parseInt(raw);
			if (value < 5) {
				System.out.println("La precision minima es 5.");
				return null;
			}
			return Integer.valueOf(value);
		} catch (NumberFormatException ex) {
			System.out.println("Precision invalida: " + raw);
			return null;
		}
	}

	private static void launchGui() {
		System.out.println("Abriendo interfaz grafica...");
		BigMathCalculatorApp.launch();
	}

	private static void printMainHelp() {
		System.out.println("Opciones:");
		System.out.println("  1 | calc        -> abrir calculadora de consola");
		System.out.println("  2 | demo        -> mostrar ejemplos de documentacion");
		System.out.println("  gui             -> lanzar la interfaz grafica");
		System.out.println("  help            -> mostrar esta ayuda");
		System.out.println("  exit            -> salir");
	}

	private static void printCalculatorHelp() {
		System.out.println("Comandos de calculadora:");
		System.out.println("  gui             -> abrir la interfaz grafica");
		System.out.println("  precision N     -> cambiar precision");
		System.out.println("  mode real       -> solo resultados reales");
		System.out.println("  mode complex    -> permitir resultados complejos");
		System.out.println("  ans             -> mostrar el ultimo resultado");
		System.out.println("  back            -> volver al menu principal");
		System.out.println("  exit            -> salir");
	}

	private static void runDemo() {
		exampleForDocu();
		System.out.println();
		exampleForJavaDoc_roundTrailingZeroes();
	}

	public static void exampleForDocu() {
		MathContext mathContext = new MathContext(100);
		
		System.out.println("All calculations with a precision of " + mathContext.getPrecision() + " digits.");
		System.out.println();
		System.out.println("Advanced functions:");
		System.out.println("  sqrt(2)        = " + BigDecimalMath.sqrt(BigDecimal.valueOf(2), mathContext));
		System.out.println("  root(2, 3)     = " + BigDecimalMath.root(BigDecimal.valueOf(2), BigDecimal.valueOf(3), mathContext));
		System.out.println("  pow(2, 3)      = " + BigDecimalMath.pow(BigDecimal.valueOf(2), BigDecimal.valueOf(3), mathContext));
		System.out.println("  pow(2.1, 3.4)  = " + BigDecimalMath.pow(BigDecimal.valueOf(2.1), BigDecimal.valueOf(3.4), mathContext));
		System.out.println("  log(2)         = " + BigDecimalMath.log(BigDecimal.valueOf(2), mathContext));
		System.out.println("  log2(2)        = " + BigDecimalMath.log2(BigDecimal.valueOf(2), mathContext));
		System.out.println("  log10(2)       = " + BigDecimalMath.log10(BigDecimal.valueOf(2), mathContext));
		System.out.println("  exp(2)         = " + BigDecimalMath.exp(BigDecimal.valueOf(2), mathContext));
		System.out.println("  sin(2)         = " + BigDecimalMath.sin(BigDecimal.valueOf(2), mathContext));
		System.out.println("  cos(2)         = " + BigDecimalMath.cos(BigDecimal.valueOf(2), mathContext));
		System.out.println("  tan(2)         = " + BigDecimalMath.tan(BigDecimal.valueOf(2), mathContext));
		System.out.println("  cot(2)         = " + BigDecimalMath.cot(BigDecimal.valueOf(2), mathContext));
		System.out.println("  asin(0.1)      = " + BigDecimalMath.asin(BigDecimal.valueOf(0.1), mathContext));
		System.out.println("  acos(0.1)      = " + BigDecimalMath.acos(BigDecimal.valueOf(0.1), mathContext));
		System.out.println("  atan(0.1)      = " + BigDecimalMath.atan(BigDecimal.valueOf(0.1), mathContext));
		System.out.println("  acot(0.1)      = " + BigDecimalMath.acot(BigDecimal.valueOf(0.1), mathContext));
		System.out.println("  sinh(2)        = " + BigDecimalMath.sinh(BigDecimal.valueOf(2), mathContext));
		System.out.println("  cosh(2)        = " + BigDecimalMath.cosh(BigDecimal.valueOf(2), mathContext));
		System.out.println("  tanh(2)        = " + BigDecimalMath.tanh(BigDecimal.valueOf(2), mathContext));
		System.out.println("  asinh(0.1)     = " + BigDecimalMath.asinh(BigDecimal.valueOf(0.1), mathContext));
		System.out.println("  acosh(2)       = " + BigDecimalMath.acosh(BigDecimal.valueOf(2), mathContext));
		System.out.println("  atanh(0.1)     = " + BigDecimalMath.atanh(BigDecimal.valueOf(0.1), mathContext));
		System.out.println("  factorial(6)   = " + BigDecimalMath.factorial(6));
		System.out.println();
		System.out.println("Constants:");
		System.out.println("  pi             = " + BigDecimalMath.pi(mathContext));
		System.out.println("  e              = " + BigDecimalMath.e(mathContext));
		System.out.println();
		System.out.println("Useful BigDecimal methods:");
		System.out.println("  mantissa(1.456E99)      = " + BigDecimalMath.mantissa(BigDecimal.valueOf(1.456E99)));
		System.out.println("  exponent(1.456E99)      = " + BigDecimalMath.exponent(BigDecimal.valueOf(1.456E99)));
		System.out.println("  integralPart(123.456)   = " + BigDecimalMath.integralPart(BigDecimal.valueOf(123.456)));
		System.out.println("  fractionalPart(123.456) = " + BigDecimalMath.fractionalPart(BigDecimal.valueOf(123.456)));
		System.out.println("  isIntValue(123)         = " + BigDecimalMath.isIntValue(BigDecimal.valueOf(123)));
		System.out.println("  isIntValue(123.456)     = " + BigDecimalMath.isIntValue(BigDecimal.valueOf(123.456)));
	}

	private static void exampleForJavaDoc_roundTrailingZeroes() {
		MathContext mc = new MathContext(5);
		System.out.println(BigDecimalMath.roundWithTrailingZeroes(new BigDecimal("1.234567"), mc));    // 1.2346
		System.out.println(BigDecimalMath.roundWithTrailingZeroes(new BigDecimal("123.4567"), mc));    // 123.46
		System.out.println(BigDecimalMath.roundWithTrailingZeroes(new BigDecimal("0.001234567"), mc)); // 0.0012346
		System.out.println(BigDecimalMath.roundWithTrailingZeroes(new BigDecimal("1.23"), mc));        // 1.2300
		System.out.println(BigDecimalMath.roundWithTrailingZeroes(new BigDecimal("1.230000"), mc));    // 1.2300
		System.out.println(BigDecimalMath.roundWithTrailingZeroes(new BigDecimal("0.00123"), mc));     // 0.0012300
		System.out.println(BigDecimalMath.roundWithTrailingZeroes(new BigDecimal("0"), mc));           // 0.0000
		System.out.println(BigDecimalMath.roundWithTrailingZeroes(new BigDecimal("0.00000000"), mc));  // 0.0000
	}
}
