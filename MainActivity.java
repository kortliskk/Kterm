package com.kortlis.kterm;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(5, 7, 6);
    private static final int PANEL = Color.rgb(12, 17, 13);
    private static final int GREEN = Color.rgb(114, 255, 141);
    private static final int DIM = Color.rgb(126, 157, 132);
    private static final int WHITE = Color.rgb(220, 235, 222);
    private static final int MAX_OUTPUT_CHARS = 24000;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final StringBuilder transcript = new StringBuilder();
    private TextView output;
    private EditText commandInput;
    private ScrollView scrollView;
    private File workingDirectory;
    private boolean commandRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        workingDirectory = getFilesDir();
        buildInterface();
        append("KTerm Terminal  v1.0\n", GREEN);
        append("Android cihaz üzerinde çalışan temel kabuk. Root gerekmez.\n", DIM);
        append("Komutlar için 'help' yaz.\n\n", DIM);
        append("$ pwd\n" + workingDirectory.getAbsolutePath() + "\n\n", WHITE);
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(15), dp(12), dp(15), dp(12));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("KTERM");
        title.setTextColor(GREEN);
        title.setTextSize(20);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(34), 1));

        TextView badge = new TextView(this);
        badge.setText("ANDROID SHELL");
        badge.setTextColor(DIM);
        badge.setTextSize(10);
        badge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        badge.setBackground(roundRect(PANEL, dp(8), Color.rgb(28, 48, 32)));
        header.addView(badge, new LinearLayout.LayoutParams(-2, -2));
        root.addView(header);

        TextView subtitle = new TextView(this);
        subtitle.setText("LOCAL TERMINAL  •  NO ROOT");
        subtitle.setTextColor(DIM);
        subtitle.setTextSize(10);
        subtitle.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.setMargins(0, dp(2), 0, dp(12));
        root.addView(subtitle, subParams);

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(roundRect(PANEL, dp(10), Color.rgb(22, 32, 24)));
        output = new TextView(this);
        output.setTextColor(WHITE);
        output.setTextSize(13);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextIsSelectable(true);
        output.setPadding(dp(13), dp(13), dp(13), dp(18));
        output.setGravity(Gravity.TOP | Gravity.START);
        scrollView.addView(output, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams consoleParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        consoleParams.setMargins(0, 0, 0, dp(12));
        root.addView(scrollView, consoleParams);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(dp(10), 0, dp(5), 0);
        inputRow.setBackground(roundRect(PANEL, dp(10), Color.rgb(30, 55, 35)));

        TextView prompt = new TextView(this);
        prompt.setText("$ ");
        prompt.setTextColor(GREEN);
        prompt.setTextSize(15);
        prompt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        inputRow.addView(prompt, new LinearLayout.LayoutParams(-2, dp(48)));

        commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setTextColor(GREEN);
        commandInput.setHintTextColor(DIM);
        commandInput.setHint("komut yaz...");
        commandInput.setTextSize(14);
        commandInput.setTypeface(Typeface.MONOSPACE);
        commandInput.setPadding(0, 0, dp(4), 0);
        commandInput.setBackgroundColor(Color.TRANSPARENT);
        commandInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        inputRow.addView(commandInput, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(inputRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(-1, dp(44));
        buttonsParams.setMargins(0, dp(8), 0, 0);
        root.addView(buttons, buttonsParams);

        Button clearButton = makeButton("TEMİZLE", false);
        buttons.addView(clearButton, new LinearLayout.LayoutParams(0, -1, 1f));
        View gap = new View(this);
        buttons.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        Button runButton = makeButton("ÇALIŞTIR  ↵", true);
        buttons.addView(runButton, new LinearLayout.LayoutParams(0, -1, 1.35f));

        runButton.setOnClickListener(v -> submitCommand());
        clearButton.setOnClickListener(v -> {
            transcript.setLength(0);
            output.setText("");
            append("Konsol temizlendi.\n", DIM);
        });
        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                submitCommand();
                return true;
            }
            return false;
        });
    }

    private Button makeButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(roundRect(primary ? GREEN : PANEL, dp(9), primary ? GREEN : Color.rgb(35, 55, 39)));
        button.setTextColor(primary ? BG : GREEN);
        return button;
    }

    private void submitCommand() {
        if (commandRunning) return;
        String command = commandInput.getText().toString().trim();
        if (command.isEmpty()) return;
        commandInput.setText("");
        append("$ " + command + "\n", GREEN);

        if (command.equals("clear") || command.equals("cls")) {
            transcript.setLength(0);
            output.setText("");
            return;
        }
        if (command.equals("help")) {
            append("KTerm komutları:\n", WHITE);
            append("help      yardım\nclear     ekranı temizle\npwd       geçerli klasör\ncd DIR    klasör değiştir\nexit      kabuk komutu\n\n", DIM);
            append("Bunların dışında komutlar Android'in /system/bin/sh\nkabuğunda çalıştırılır. Her komut için yeni kabuk açılır.\nPython, apt/pkg ve tam Linux dağıtımı dahil değildir.\n\n", DIM);
            return;
        }
        if (command.equals("pwd")) {
            append(workingDirectory.getAbsolutePath() + "\n\n", WHITE);
            return;
        }
        if (command.equals("cd") || command.startsWith("cd ")) {
            String destination = command.length() > 2 ? command.substring(2).trim() : "";
            if (destination.isEmpty() || destination.equals("~")) {
                workingDirectory = getFilesDir();
            } else {
                if ((destination.startsWith("\"") && destination.endsWith("\"")) ||
                    (destination.startsWith("'") && destination.endsWith("'"))) {
                    destination = destination.substring(1, destination.length() - 1);
                }
                File next = new File(destination.startsWith("/") ? destination : new File(workingDirectory, destination).getPath());
                try {
                    next = next.getCanonicalFile();
                    if (next.isDirectory() && next.canRead()) {
                        workingDirectory = next;
                    } else {
                        append("cd: klasör bulunamadı veya erişilemiyor: " + destination + "\n\n", Color.rgb(255, 133, 133));
                        return;
                    }
                } catch (Exception e) {
                    append("cd: " + e.getMessage() + "\n\n", Color.rgb(255, 133, 133));
                    return;
                }
            }
            append(workingDirectory.getAbsolutePath() + "\n\n", WHITE);
            return;
        }

        commandRunning = true;
        commandInput.setEnabled(false);
        executor.execute(() -> {
            String result = executeShell(command);
            mainHandler.post(() -> {
                append(result, result.startsWith("[KTerm]") ? Color.rgb(255, 190, 100) : WHITE);
                if (!result.endsWith("\n")) append("\n", WHITE);
                append("\n", WHITE);
                commandRunning = false;
                commandInput.setEnabled(true);
                commandInput.requestFocus();
            });
        });
    }

    private String executeShell(String command) {
        File outputFile = null;
        Process process = null;
        try {
            outputFile = File.createTempFile("kterm-output-", ".txt", getCacheDir());
            ProcessBuilder builder = new ProcessBuilder("/system/bin/sh", "-c", command);
            builder.directory(workingDirectory);
            builder.redirectErrorStream(true);
            builder.redirectOutput(outputFile);
            builder.environment().put("HOME", getFilesDir().getAbsolutePath());
            builder.environment().put("TERM", "xterm-256color");
            process = builder.start();
            boolean finished = process.waitFor(8, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
            }
            byte[] bytes;
            try (FileInputStream input = new FileInputStream(outputFile);
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[4096];
                int count;
                while ((count = input.read(chunk)) != -1) buffer.write(chunk, 0, count);
                bytes = buffer.toByteArray();
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > MAX_OUTPUT_CHARS) {
                text = text.substring(0, MAX_OUTPUT_CHARS) + "\n[KTerm] Çıktı uzun olduğu için kısaltıldı.\n";
            }
            if (!finished) text += "\n[KTerm] Komut 8 saniyelik süre sınırını aştı ve durduruldu.\n";
            return text.isEmpty() ? "(çıktı yok)\n" : text;
        } catch (Exception e) {
            return "[KTerm] Komut çalıştırılamadı: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n";
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            if (outputFile != null) outputFile.delete();
        }
    }

    private void append(String text, int color) {
        transcript.append(text);
        if (transcript.length() > MAX_OUTPUT_CHARS) {
            transcript.delete(0, transcript.length() - MAX_OUTPUT_CHARS);
        }
        output.setText(transcript.toString());
        output.setTextColor(GREEN);
        // Monospace terminal output uses the classic green-on-black look.
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        if (strokeColor != color) shape.setStroke(dp(1), strokeColor);
        return shape;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
