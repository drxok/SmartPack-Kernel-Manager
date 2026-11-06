package com.smartpack.kernelmanager.utils;

import com.smartpack.kernelmanager.utils.root.RootUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Kernel Dr.xok: leitura rápida de arquivos do sistema (direto quando dá, root quando precisa).
 * Uso só em segundo plano.
 */
public class DxInfo {

    private static final Set<String> sNeedsRoot = new HashSet<>();

    public static String read(String path) {
        boolean root;
        synchronized (sNeedsRoot) {
            root = sNeedsRoot.contains(path);
        }
        if (!root) {
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(line);
                }
                return sb.toString().trim();
            } catch (Exception e) {
                synchronized (sNeedsRoot) {
                    sNeedsRoot.add(path);
                }
            }
        }
        try {
            String out = RootUtils.runAndGetOutput("cat '" + path + "' 2>/dev/null");
            return out == null ? "" : out.trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static int readInt(String path) {
        try {
            return Integer.parseInt(read(path).split("\\s+")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    /** "[zstd] lz4" -> "zstd"; "noop [deadline] cfq" -> "deadline" */
    public static String bracketed(String text) {
        int a = text.indexOf('['), b = text.indexOf(']');
        if (a >= 0 && b > a) return text.substring(a + 1, b);
        return text.trim();
    }

    public static boolean exists(String path) {
        if (new File(path).exists()) return true;
        try {
            return "1".equals(RootUtils.runAndGetOutput("[ -e '" + path + "' ] && echo 1"));
        } catch (Exception e) {
            return false;
        }
    }
}
