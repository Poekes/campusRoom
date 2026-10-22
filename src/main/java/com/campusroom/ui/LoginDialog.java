package com.campusroom.ui;

import com.campusroom.model.User;
import com.campusroom.service.AuthService;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * LoginDialog - Jendela Dialog Login Modern dengan Contoh Akun Demo Bawaan.
 * Memungkinkan login manual atau login cepat 1-klik dengan akun Mahasiswa, Sekdek, dan Admin.
 */
public class LoginDialog extends JDialog {
    public interface LoginSuccessCallback {
        void onLoginSuccess(User user);
    }

    private final LoginSuccessCallback callback;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private JCheckBox chkShowPassword;

    public LoginDialog(Frame parent, LoginSuccessCallback callback) {
        super(parent, "CampusRoom — Masuk ke Sistem", true);
        this.callback = callback;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(560, 710);
        setMinimumSize(new Dimension(520, 680));
        setLocationRelativeTo(parent);
        setResizable(false);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (!AuthService.getInstance().isLoggedIn()) {
                    System.exit(0);
                }
            }
        });

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(248, 250, 252));

        // 1. Top Header Banner
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Content (Scrollable if needed)
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(248, 250, 252));
        contentPanel.setBorder(new EmptyBorder(15, 25, 20, 25));

        // Section A: Contoh Akun Demo Cepat
        contentPanel.add(createDemoAccountsSection());

        contentPanel.add(Box.createVerticalStrut(15));

        // Section B: Divider
        contentPanel.add(createDividerPanel("Atau Masuk Menggunakan Akun Manual"));

        contentPanel.add(Box.createVerticalStrut(12));

        // Section C: Form Login Manual
        contentPanel.add(createManualLoginForm());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Footer
        mainPanel.add(createFooterPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42)); // Slate Navy 900
        header.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        brandPanel.setOpaque(false);

        JPanel logoBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(59, 130, 246), getWidth(), getHeight(), new Color(99, 102, 241));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        logoBox.setPreferredSize(new Dimension(42, 42));
        logoBox.setOpaque(false);

        JLabel lblLogo = new JLabel("CR", JLabel.CENTER);
        lblLogo.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblLogo.setForeground(Color.WHITE);
        logoBox.add(lblLogo);
        brandPanel.add(logoBox);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("CampusRoom Login");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("Sistem Manajemen & Reservasi Ruangan Kuliah Multi-Kampus");
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(148, 163, 184));

        textPanel.add(lblTitle);
        textPanel.add(lblSubtitle);
        brandPanel.add(textPanel);

        header.add(brandPanel, BorderLayout.CENTER);
        return header;
    }

    private JPanel createDemoAccountsSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel lblSection = new JLabel("Pilih Akun Demo (Masuk Instan):");
        lblSection.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblSection.setForeground(new Color(30, 41, 59));
        lblSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblSection);

        panel.add(Box.createVerticalStrut(8));

        // Demo Card 1: Mahasiswa
        panel.add(createDemoAccountCard(
                "Mahasiswa / PJ Kuliah",
                "mahasiswa",
                "123",
                "Muhamad Nur Akbar (NIM: 250111019)",
                "Cek denah real-time, lihat ruangan kosong/terpakai, dan ajukan booking kuliah.",
                new Color(2, 132, 199),
                new Color(240, 249, 255)
        ));

        panel.add(Box.createVerticalStrut(8));

        // Demo Card 2: Sekretaris Dekan
        panel.add(createDemoAccountCard(
                "Sekretaris Dekan (Sekdek)",
                "sekdek",
                "123",
                "Ibu Nurul Hidayah, S.Kom.",
                "Validasi permohonan mahasiswa dan kelola master jadwal tetap dekanat.",
                new Color(124, 58, 237),
                new Color(245, 243, 255)
        ));

        panel.add(Box.createVerticalStrut(8));

        // Demo Card 3: Admin Master
        panel.add(createDemoAccountCard(
                "Administrator Master (Admin)",
                "admin",
                "123",
                "Bpk. Rahmat Santoso",
                "Approval final, kelola multi-kampus & gedung, serta editor visual layout denah.",
                new Color(5, 150, 105),
                new Color(236, 253, 245)
        ));

        return panel;
    }

    private JPanel createDemoAccountCard(String roleTitle, String username, String password,
                                         String name, String desc, Color primaryColor, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 86));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Center: Details
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        centerPanel.setOpaque(false);

        JLabel lblRole = new JLabel(roleTitle);
        lblRole.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRole.setForeground(primaryColor);

        JLabel lblNameCred = new JLabel("Nama: " + name + "  |  User: " + username + "  Pass: " + password);
        lblNameCred.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblNameCred.setForeground(new Color(51, 65, 85));

        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblDesc.setForeground(new Color(100, 116, 139));

        centerPanel.add(lblRole);
        centerPanel.add(lblNameCred);
        centerPanel.add(lblDesc);
        card.add(centerPanel, BorderLayout.CENTER);

        // East: Quick Button
        JButton btnQuickLogin = new JButton("Gunakan ➔");
        btnQuickLogin.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnQuickLogin.setBackground(primaryColor);
        btnQuickLogin.setForeground(Color.WHITE);
        btnQuickLogin.setFocusPainted(false);
        btnQuickLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnQuickLogin.addActionListener(e -> {
            txtUsername.setText(username);
            txtPassword.setText(password);
            performLogin(username, password);
        });

        JPanel btnWrapper = new JPanel(new GridBagLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.add(btnQuickLogin);
        card.add(btnWrapper, BorderLayout.EAST);

        return card;
    }

    private JPanel createDividerPanel(String text) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Short.MAX_VALUE, 24));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new Color(203, 213, 225));
        p.add(sep1, gbc);

        JLabel lbl = new JLabel("  " + text + "  ");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(new Color(100, 116, 139));
        gbc.weightx = 0;
        p.add(lbl, gbc);

        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(203, 213, 225));
        gbc.weightx = 1.0;
        p.add(sep2, gbc);

        return p;
    }

    private JPanel createManualLoginForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(14, 16, 14, 16)
        ));
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Username
        JLabel lblUser = new JLabel("Username Pengguna:");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblUser.setForeground(new Color(51, 65, 85));
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblUser);
        form.add(Box.createVerticalStrut(4));

        txtUsername = new JTextField();
        txtUsername.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtUsername.setMaximumSize(new Dimension(Short.MAX_VALUE, 32));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtUsername);

        form.add(Box.createVerticalStrut(10));

        // Password
        JLabel lblPass = new JLabel("Kata Sandi (Password):");
        lblPass.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblPass.setForeground(new Color(51, 65, 85));
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblPass);
        form.add(Box.createVerticalStrut(4));

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtPassword.setMaximumSize(new Dimension(Short.MAX_VALUE, 32));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtPassword);

        form.add(Box.createVerticalStrut(6));

        // Show password checkbox
        chkShowPassword = new JCheckBox("Tampilkan Kata Sandi");
        chkShowPassword.setFont(new Font("SansSerif", Font.PLAIN, 11));
        chkShowPassword.setOpaque(false);
        chkShowPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkShowPassword.addActionListener(e -> {
            if (chkShowPassword.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('•');
            }
        });
        form.add(chkShowPassword);

        form.add(Box.createVerticalStrut(8));

        // Error message label
        lblError = new JLabel(" ");
        lblError.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblError.setForeground(new Color(220, 38, 38));
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblError);

        form.add(Box.createVerticalStrut(8));

        // Submit Button
        JButton btnLogin = new JButton("🔐 Masuk ke Sistem");
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLogin.setBackground(new Color(37, 99, 235)); // Royal Blue
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setMaximumSize(new Dimension(Short.MAX_VALUE, 38));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> {
            String u = txtUsername.getText().trim();
            String p = new String(txtPassword.getPassword()).trim();
            performLogin(u, p);
        });
        form.add(btnLogin);

        // Enter key listeners
        KeyAdapter enterHandler = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String u = txtUsername.getText().trim();
                    String p = new String(txtPassword.getPassword()).trim();
                    performLogin(u, p);
                }
            }
        };
        txtUsername.addKeyListener(enterHandler);
        txtPassword.addKeyListener(enterHandler);

        return form;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(241, 245, 249));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JLabel lbl = new JLabel("CampusRoom v1.2 — Sistem Manajemen & Reservasi Ruangan Terpadu");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(new Color(100, 116, 139));
        footer.add(lbl);

        return footer;
    }

    private void performLogin(String username, String password) {
        if (username.isEmpty()) {
            lblError.setText("⚠️ Username tidak boleh kosong!");
            txtUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            lblError.setText("⚠️ Password tidak boleh kosong!");
            txtPassword.requestFocus();
            return;
        }

        lblError.setText(" ");
        boolean ok = AuthService.getInstance().login(username, password);
        if (ok) {
            User user = AuthService.getInstance().getCurrentUser();
            dispose();
            if (callback != null) {
                callback.onLoginSuccess(user);
            }
        } else {
            lblError.setText("❌ Username atau password salah! Silakan coba lagi.");
        }
    }
}
