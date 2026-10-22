package com.campusroom.ui;

import com.campusroom.model.User;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * SidebarNavigationPanel - Komponen Navigasi Sidebar Interaktif Modern.
 * Menggantikan tab bar horizontal dengan sidebar vertikal elegan bergaya SaaS/Enterprise.
 * Mendukung fitur:
 * 1. Penuh (Expanded ~265px) dan Ciut/Minimize (Compact Icon-Only Rail ~70px).
 * 2. 9 menu lengkap sesuai peran pengguna, highlight aktif, hover responsif.
 * 3. Badge counter dinamis (notifikasi validasi & approval) dengan indikator titik di mode ciut.
 * 4. Kartu profil pengguna dan tombol akses cepat peran / keluar.
 */
public class SidebarNavigationPanel extends JPanel {

    // Lebar sidebar
    public static final int COLLAPSED_WIDTH = 70;
    public static final int EXPANDED_WIDTH = 265;

    // 9 Navigasi Interaktif Menu
    public static final int NAV_FLOOR_PLAN = 0;       // Denah Visual
    public static final int NAV_CATALOG = 1;          // Katalog Ruangan
    public static final int NAV_BOOKING = 2;          // Riwayat & Pengajuan Saya
    public static final int NAV_SEC_VALIDATION = 3;   // Validasi Pengajuan Sekdek
    public static final int NAV_SEC_SCHEDULE = 4;     // Jadwal Tetap Perkuliahan
    public static final int NAV_ADMIN_APPROVAL = 5;   // Approval Akhir Admin Master
    public static final int NAV_ADMIN_CAMPUS = 6;     // Kelola Kampus & Gedung
    public static final int NAV_ADMIN_ROOMS = 7;      // Kelola Master Ruangan
    public static final int NAV_LAYOUT_EDITOR = 8;    // Visual Ribbon Desainer Denah

    // Backward-compatibility aliases for legacy indices
    public static final int NAV_SECRETARY = NAV_SEC_VALIDATION;
    public static final int NAV_ADMIN = NAV_ADMIN_APPROVAL;

    public interface NavigationListener {
        void onNavigationSelected(int navIndex);
        void onSwitchRoleRequested();
        void onLogoutRequested();
        default void onSidebarCollapseToggled(boolean isCollapsed) {}
    }

    private final NavigationListener listener;
    private final List<NavItemView> navItems = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean isCollapsed = false;
    private String currentRoleStr = "MAHASISWA";

    // Section Containers, Headers, Dividers & Spacers
    private JPanel panelSectionMain;
    private JPanel panelSectionSecretary;
    private JPanel panelSectionAdmin;
    private Component spacerSecretary;
    private Component spacerAdmin;
    private JPanel panelStudentInfo;
    private final List<JLabel> sectionHeaderLabels = new ArrayList<>();
    private final List<JComponent> sectionDividers = new ArrayList<>();

    // Header UI
    private JPanel brandHeader;
    private JPanel logoBox;
    private JPanel textCol;
    private JButton btnToggleCollapse;

    // Center scrollable menu
    private JPanel menuContainer;
    private JScrollPane scrollPane;

    // Profile Card UI
    private JPanel profileWrapper;
    private JPanel profileCard;
    private JPanel userRow;
    private JPanel infoCol;
    private JPanel actionRow;
    private JPanel avatarContainer;
    private JLabel lblUserName;
    private JLabel lblUserRole;
    private User currentUser;

    public SidebarNavigationPanel(NavigationListener listener) {
        this.listener = listener;
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 760));
        setMinimumSize(new Dimension(250, 600));
        setBackground(ModernUIHelper.SIDEBAR_BG);
        setLayout(new BorderLayout());

        initUI();
    }

    private void initUI() {
        // 1. Top Brand Header
        add(createBrandHeader(), BorderLayout.NORTH);

        // 2. Center Scrollable Navigation Items
        menuContainer = new JPanel();
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setOpaque(false);
        menuContainer.setBorder(new EmptyBorder(12, 10, 12, 10));

        // Group 1: Navigasi Utama (Semua Peran: Mahasiswa, Sekdek, Admin)
        panelSectionMain = new JPanel();
        panelSectionMain.setLayout(new BoxLayout(panelSectionMain, BoxLayout.Y_AXIS));
        panelSectionMain.setOpaque(false);
        panelSectionMain.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelSectionMain.add(createSectionHeader("NAVIGASI UTAMA", false));
        addNavItem(panelSectionMain, NAV_FLOOR_PLAN, ModernUIHelper.VectorIcon.Type.FLOOR_PLAN, "Denah Interaktif", "Peta denah visual & status");
        addNavItem(panelSectionMain, NAV_CATALOG, ModernUIHelper.VectorIcon.Type.CATALOG, "Katalog Ruangan", "Daftar spesifikasi lengkap");
        addNavItem(panelSectionMain, NAV_BOOKING, ModernUIHelper.VectorIcon.Type.BOOKING, "Pengajuan Saya", "Riwayat & status reservasi");
        menuContainer.add(panelSectionMain);

        spacerSecretary = Box.createVerticalStrut(12);
        menuContainer.add(spacerSecretary);

        // Group 2: Portal Dekanat (Sekdek & Admin)
        panelSectionSecretary = new JPanel();
        panelSectionSecretary.setLayout(new BoxLayout(panelSectionSecretary, BoxLayout.Y_AXIS));
        panelSectionSecretary.setOpaque(false);
        panelSectionSecretary.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelSectionSecretary.add(createSectionHeader("PORTAL SEKRETARIS", true));
        addNavItem(panelSectionSecretary, NAV_SEC_VALIDATION, ModernUIHelper.VectorIcon.Type.VALIDATION, "Validasi Pengajuan", "Verifikasi jadwal mahasiswa");
        addNavItem(panelSectionSecretary, NAV_SEC_SCHEDULE, ModernUIHelper.VectorIcon.Type.SCHEDULE, "Jadwal Perkuliahan", "Master jadwal tetap semester");
        menuContainer.add(panelSectionSecretary);

        spacerAdmin = Box.createVerticalStrut(12);
        menuContainer.add(spacerAdmin);

        // Group 3: Administrasi Sistem (Admin Master)
        panelSectionAdmin = new JPanel();
        panelSectionAdmin.setLayout(new BoxLayout(panelSectionAdmin, BoxLayout.Y_AXIS));
        panelSectionAdmin.setOpaque(false);
        panelSectionAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelSectionAdmin.add(createSectionHeader("ADMINISTRASI SISTEM", true));
        addNavItem(panelSectionAdmin, NAV_ADMIN_APPROVAL, ModernUIHelper.VectorIcon.Type.APPROVAL, "Approval Akhir", "Persetujuan final reservasi");
        addNavItem(panelSectionAdmin, NAV_ADMIN_CAMPUS, ModernUIHelper.VectorIcon.Type.CAMPUS, "Kampus & Gedung", "Master multi-lokasi kampus");
        addNavItem(panelSectionAdmin, NAV_ADMIN_ROOMS, ModernUIHelper.VectorIcon.Type.ROOMS, "Master Ruangan", "Inventaris & kapasitas");
        addNavItem(panelSectionAdmin, NAV_LAYOUT_EDITOR, ModernUIHelper.VectorIcon.Type.EDITOR, "Desainer Denah", "Editor tata letak visual");
        menuContainer.add(panelSectionAdmin);

        // Group 4: Student Info Card (hanya terlihat saat peran Mahasiswa)
        panelStudentInfo = createStudentInfoCard();
        panelStudentInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        menuContainer.add(panelStudentInfo);

        menuContainer.add(Box.createVerticalGlue());

        scrollPane = new JScrollPane(menuContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom User Profile Card
        add(createUserProfileCard(), BorderLayout.SOUTH);

        // Default selection: Denah
        selectItem(0, false);
    }

    private JPanel createBrandHeader() {
        brandHeader = new JPanel(new BorderLayout(8, 0));
        brandHeader.setOpaque(false);
        brandHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernUIHelper.SIDEBAR_BORDER),
                new EmptyBorder(14, 12, 14, 12)
        ));

        // Logo Box (36x36) dengan inisial CR dan latar gradien modern
        logoBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(59, 130, 246), getWidth(), getHeight(), new Color(99, 102, 241));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        logoBox.setPreferredSize(new Dimension(36, 36));
        logoBox.setMaximumSize(new Dimension(36, 36));
        logoBox.setOpaque(false);
        logoBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoBox.setToolTipText("CampusRoom");

        JLabel lblLogo = new JLabel("CR", JLabel.CENTER);
        lblLogo.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoBox.add(lblLogo);

        MouseAdapter logoClickAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isCollapsed) {
                    setCollapsed(false);
                }
            }
        };
        logoBox.addMouseListener(logoClickAdapter);
        lblLogo.addMouseListener(logoClickAdapter);
        brandHeader.add(logoBox, BorderLayout.WEST);

        // Kolom Nama Aplikasi & Badge
        textCol = new JPanel(new GridLayout(2, 1, 0, 2));
        textCol.setOpaque(false);

        JPanel titleBadgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleBadgeRow.setOpaque(false);

        JLabel lblTitle = new JLabel("CampusRoom");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitle.setForeground(ModernUIHelper.TEXT_WHITE);
        titleBadgeRow.add(lblTitle);

        JLabel lblVer = new JLabel("PRO") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblVer.setFont(new Font("SansSerif", Font.BOLD, 9));
        lblVer.setOpaque(false);
        lblVer.setBackground(new Color(30, 58, 138));
        lblVer.setForeground(new Color(147, 197, 253));
        lblVer.setBorder(new EmptyBorder(2, 6, 2, 6));
        titleBadgeRow.add(lblVer);

        JLabel lblSub = new JLabel("Sistem Reservasi Terpadu");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblSub.setForeground(ModernUIHelper.TEXT_MUTED);

        textCol.add(titleBadgeRow);
        textCol.add(lblSub);
        brandHeader.add(textCol, BorderLayout.CENTER);

        // Toggle button collapse / expand
        btnToggleCollapse = new JButton("«") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnToggleCollapse.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnToggleCollapse.setForeground(new Color(148, 163, 184));
        btnToggleCollapse.setFocusPainted(false);
        btnToggleCollapse.setBorderPainted(false);
        btnToggleCollapse.setContentAreaFilled(false);
        btnToggleCollapse.setOpaque(false);
        btnToggleCollapse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleCollapse.setPreferredSize(new Dimension(28, 28));
        btnToggleCollapse.setMaximumSize(new Dimension(28, 28));
        btnToggleCollapse.setToolTipText("Ciutkan Sidebar (Minimize)");
        btnToggleCollapse.addActionListener(e -> toggleCollapse());

        brandHeader.add(btnToggleCollapse, BorderLayout.EAST);

        return brandHeader;
    }

    private JPanel createSectionHeader(String text, boolean hasDivider) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(new Color(100, 116, 139)); // Slate 500
        lbl.setBorder(new EmptyBorder(10, 10, 6, 10));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        sectionHeaderLabels.add(lbl);
        container.add(lbl);

        if (hasDivider) {
            JPanel divWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            divWrapper.setOpaque(false);
            divWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
            JSeparator sep = new JSeparator();
            sep.setForeground(ModernUIHelper.SIDEBAR_BORDER);
            sep.setBackground(ModernUIHelper.SIDEBAR_BORDER);
            sep.setPreferredSize(new Dimension(42, 1));
            divWrapper.add(sep);
            divWrapper.setVisible(false);
            sectionDividers.add(divWrapper);
            container.add(divWrapper);
        }

        return container;
    }

    private void addNavItem(JPanel container, int navIndex, ModernUIHelper.VectorIcon.Type iconType, String title, String subtitle) {
        NavItemView item = new NavItemView(navIndex, iconType, title, subtitle);
        navItems.add(item);
        item.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(item);
        container.add(Box.createVerticalStrut(3));
    }

    private JPanel createStudentInfoCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ModernUIHelper.SIDEBAR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(51, 65, 85, 120));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 160));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header: Badge info
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        topRow.setOpaque(false);
        JLabel lblBadge = new JLabel("PANDUAN RESERVASI") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblBadge.setOpaque(false);
        lblBadge.setBackground(new Color(14, 165, 233, 40));
        lblBadge.setForeground(new Color(56, 189, 248));
        lblBadge.setFont(new Font("SansSerif", Font.BOLD, 9));
        lblBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
        topRow.add(lblBadge);
        card.add(topRow, BorderLayout.NORTH);

        // Center Content: Steps
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel lblStep1 = new JLabel("• Cek ketersediaan di denah/katalog");
        lblStep1.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblStep1.setForeground(new Color(203, 213, 225));

        JLabel lblStep2 = new JLabel("• Ajukan permohonan ruangan");
        lblStep2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblStep2.setForeground(new Color(203, 213, 225));

        JLabel lblStep3 = new JLabel("• Tunggu validasi dekanat & admin");
        lblStep3.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblStep3.setForeground(new Color(203, 213, 225));

        content.add(Box.createVerticalStrut(4));
        content.add(lblStep1);
        content.add(Box.createVerticalStrut(3));
        content.add(lblStep2);
        content.add(Box.createVerticalStrut(3));
        content.add(lblStep3);
        content.add(Box.createVerticalStrut(8));

        // Status chip
        JLabel lblStatus = new JLabel("● Sesi Akademik: Aktif") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblStatus.setOpaque(false);
        lblStatus.setBackground(new Color(16, 185, 129, 30));
        lblStatus.setForeground(new Color(110, 231, 183));
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 9));
        lblStatus.setBorder(new EmptyBorder(3, 8, 3, 8));
        content.add(lblStatus);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    public JPanel getStudentInfoPanel() {
        return panelStudentInfo;
    }

    private JPanel createUserProfileCard() {
        profileWrapper = new JPanel(new BorderLayout());
        profileWrapper.setOpaque(false);
        profileWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernUIHelper.SIDEBAR_BORDER),
                new EmptyBorder(12, 12, 12, 12)
        ));

        profileCard = new JPanel(new BorderLayout(8, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(51, 65, 85, 120));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        profileCard.setOpaque(false);
        profileCard.setBackground(ModernUIHelper.SIDEBAR_CARD);
        profileCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Top: Avatar + Name + Role Badge
        userRow = new JPanel(new BorderLayout(10, 0));
        userRow.setOpaque(false);

        avatarContainer = new JPanel(new BorderLayout());
        avatarContainer.setOpaque(false);
        avatarContainer.setPreferredSize(new Dimension(36, 36));
        avatarContainer.setMaximumSize(new Dimension(36, 36));
        avatarContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarContainer.add(ModernUIHelper.createAvatarComponent("Tamu", "GUEST", 36), BorderLayout.CENTER);

        MouseAdapter profileClickAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isCollapsed) {
                    showCollapsedProfileMenu();
                }
            }
        };
        avatarContainer.addMouseListener(profileClickAdapter);
        profileCard.addMouseListener(profileClickAdapter);
        userRow.add(avatarContainer, BorderLayout.WEST);

        infoCol = new JPanel(new GridLayout(2, 1, 0, 2));
        infoCol.setOpaque(false);

        lblUserName = new JLabel("Pengguna");
        lblUserName.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblUserName.setForeground(Color.WHITE);

        lblUserRole = new JLabel("MAHASISWA");
        lblUserRole.setFont(new Font("SansSerif", Font.BOLD, 9));
        lblUserRole.setForeground(new Color(56, 189, 248)); // Sky 400

        infoCol.add(lblUserName);
        infoCol.add(lblUserRole);
        userRow.add(infoCol, BorderLayout.CENTER);
        profileCard.add(userRow, BorderLayout.NORTH);

        // Bottom: Action Buttons (Role Switcher & Logout)
        actionRow = new JPanel(new GridLayout(1, 2, 8, 0));
        actionRow.setOpaque(false);

        JButton btnSwitch = ModernUIHelper.createStyledButton("Ganti Akun", new Color(30, 41, 59), new Color(226, 232, 240), 10, false);
        btnSwitch.setToolTipText("Ganti peran demo (Mahasiswa / Sekdek / Admin)");
        btnSwitch.addActionListener(e -> {
            if (listener != null) listener.onSwitchRoleRequested();
        });

        JButton btnLogout = ModernUIHelper.createStyledButton("Keluar", new Color(239, 68, 68, 30), new Color(252, 165, 165), 10, false);
        btnLogout.setToolTipText("Keluar dari aplikasi ke layar login");
        btnLogout.addActionListener(e -> {
            if (listener != null) listener.onLogoutRequested();
        });

        actionRow.add(btnSwitch);
        actionRow.add(btnLogout);
        profileCard.add(actionRow, BorderLayout.SOUTH);

        profileWrapper.add(profileCard, BorderLayout.CENTER);
        return profileWrapper;
    }

    private void showCollapsedProfileMenu() {
        JPopupMenu menu = new JPopupMenu();

        String name = (currentUser != null) ? currentUser.getFullName() : "Pengguna Tamu";
        String role = (currentUser != null) ? currentUser.getRoleDisplayName() : "GUEST";

        JMenuItem headerItem = new JMenuItem("👤 " + name + " (" + role + ")");
        headerItem.setEnabled(false);
        headerItem.setFont(new Font("SansSerif", Font.BOLD, 11));
        menu.add(headerItem);
        menu.addSeparator();

        JMenu switchMenu = new JMenu("Ganti Akun Demo (Switch Role)");
        switchMenu.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JMenuItem s1 = new JMenuItem("Mahasiswa / PJ Kuliah (Muhamad Nur Akbar)");
        s1.addActionListener(e -> com.campusroom.service.AuthService.getInstance().switchUserById(1));
        switchMenu.add(s1);

        JMenuItem s2 = new JMenuItem("Sekretaris Dekan (Ibu Nurul Hidayah)");
        s2.addActionListener(e -> com.campusroom.service.AuthService.getInstance().switchUserById(2));
        switchMenu.add(s2);

        JMenuItem s3 = new JMenuItem("Administrator Master (Bpk. Rahmat Santoso)");
        s3.addActionListener(e -> com.campusroom.service.AuthService.getInstance().switchUserById(3));
        switchMenu.add(s3);

        menu.add(switchMenu);

        JMenuItem itemLogout = new JMenuItem("Keluar (Logout)");
        itemLogout.setFont(new Font("SansSerif", Font.PLAIN, 12));
        itemLogout.addActionListener(e -> {
            if (listener != null) listener.onLogoutRequested();
        });
        menu.add(itemLogout);

        menu.addSeparator();
        JMenuItem itemExpand = new JMenuItem("Perluas Sidebar (Expand)");
        itemExpand.setFont(new Font("SansSerif", Font.PLAIN, 12));
        itemExpand.addActionListener(e -> setCollapsed(false));
        menu.add(itemExpand);

        int x = getWidth() + 4;
        int y = Math.max(8, getHeight() - menu.getPreferredSize().height - 12);
        menu.show(this, x, y);
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }

    public void setCollapsed(boolean collapsed) {
        if (this.isCollapsed == collapsed) return;
        this.isCollapsed = collapsed;

        int targetWidth = collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
        int minWidth = collapsed ? COLLAPSED_WIDTH : 250;
        setPreferredSize(new Dimension(targetWidth, getHeight() > 0 ? getHeight() : 760));
        setMinimumSize(new Dimension(minWidth, 600));

        updateCollapsedUI();

        revalidate();
        repaint();

        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }

        if (listener != null) {
            listener.onSidebarCollapseToggled(collapsed);
        }
    }

    public void toggleCollapse() {
        setCollapsed(!isCollapsed);
    }

    private void updateCollapsedUI() {
        if (isCollapsed) {
            // Header: Stack Logo Box and expand button vertically in compact rail
            brandHeader.removeAll();
            brandHeader.setLayout(new BoxLayout(brandHeader, BoxLayout.Y_AXIS));
            brandHeader.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ModernUIHelper.SIDEBAR_BORDER),
                    new EmptyBorder(12, 6, 12, 6)
            ));
            logoBox.setAlignmentX(Component.CENTER_ALIGNMENT);
            logoBox.setToolTipText("CampusRoom PRO — Klik untuk memperluas sidebar");
            brandHeader.add(logoBox);
            brandHeader.add(Box.createVerticalStrut(6));
            btnToggleCollapse.setText("»");
            btnToggleCollapse.setToolTipText("Perluas Sidebar (Expand)");
            btnToggleCollapse.setAlignmentX(Component.CENTER_ALIGNMENT);
            brandHeader.add(btnToggleCollapse);

            // Container padding & scrollbar
            menuContainer.setBorder(new EmptyBorder(10, 4, 10, 4));
            scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

            // Section headers & dividers
            for (JLabel lbl : sectionHeaderLabels) lbl.setVisible(false);
            for (JComponent div : sectionDividers) div.setVisible(true);
            if (spacerSecretary != null) spacerSecretary.setVisible(false);
            if (spacerAdmin != null) spacerAdmin.setVisible(false);

            // Student Info Card: hidden in compact mode
            if (panelStudentInfo != null) {
                panelStudentInfo.setVisible(false);
            }

            // Profile Card: compact avatar only
            profileWrapper.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, ModernUIHelper.SIDEBAR_BORDER),
                    new EmptyBorder(8, 4, 8, 4)
            ));
            profileCard.removeAll();
            profileCard.setBorder(new EmptyBorder(6, 6, 6, 6));
            profileCard.setLayout(new GridBagLayout());
            profileCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            avatarContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
            profileCard.add(avatarContainer);

            String tip = (currentUser != null ? currentUser.getFullName() + " (" + currentUser.getRoleDisplayName() + ")" : "Tamu")
                    + " • Klik untuk menu akun";
            profileCard.setToolTipText(tip);
            avatarContainer.setToolTipText(tip);
        } else {
            // Header: Full brand header with logo, title, and collapse button
            brandHeader.removeAll();
            brandHeader.setLayout(new BorderLayout(8, 0));
            brandHeader.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ModernUIHelper.SIDEBAR_BORDER),
                    new EmptyBorder(14, 12, 14, 12)
            ));
            logoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            logoBox.setToolTipText("CampusRoom PRO");
            brandHeader.add(logoBox, BorderLayout.WEST);
            textCol.setVisible(true);
            brandHeader.add(textCol, BorderLayout.CENTER);
            btnToggleCollapse.setText("«");
            btnToggleCollapse.setToolTipText("Ciutkan Sidebar (Minimize)");
            brandHeader.add(btnToggleCollapse, BorderLayout.EAST);

            // Container padding & scrollbar
            menuContainer.setBorder(new EmptyBorder(12, 10, 12, 10));
            scrollPane.getVerticalScrollBar().setPreferredSize(null);

            // Section headers & dividers
            for (JLabel lbl : sectionHeaderLabels) lbl.setVisible(true);
            for (JComponent div : sectionDividers) div.setVisible(false);
            if (spacerSecretary != null && panelSectionSecretary != null) {
                spacerSecretary.setVisible(panelSectionSecretary.isVisible());
            }
            if (spacerAdmin != null && panelSectionAdmin != null) {
                spacerAdmin.setVisible(panelSectionAdmin.isVisible());
            }

            // Student Info Card
            if (panelStudentInfo != null) {
                boolean isStudent = currentRoleStr.trim().toUpperCase().contains("MAHASISWA");
                panelStudentInfo.setVisible(isStudent);
            }

            // Profile Card: Full card
            profileWrapper.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, ModernUIHelper.SIDEBAR_BORDER),
                    new EmptyBorder(12, 12, 12, 12)
            ));
            profileCard.removeAll();
            profileCard.setBorder(new EmptyBorder(10, 12, 10, 12));
            profileCard.setLayout(new BorderLayout(8, 10));
            profileCard.setCursor(Cursor.getDefaultCursor());
            userRow.removeAll();
            userRow.setLayout(new BorderLayout(10, 0));
            userRow.add(avatarContainer, BorderLayout.WEST);
            infoCol.setVisible(true);
            userRow.add(infoCol, BorderLayout.CENTER);
            profileCard.add(userRow, BorderLayout.NORTH);
            actionRow.setVisible(true);
            profileCard.add(actionRow, BorderLayout.SOUTH);
            profileCard.setToolTipText(null);
            avatarContainer.setToolTipText(null);
        }

        // NavItems
        for (NavItemView item : navItems) {
            item.setCollapsed(isCollapsed);
        }

        brandHeader.revalidate();
        brandHeader.repaint();
        profileWrapper.revalidate();
        profileWrapper.repaint();
        menuContainer.revalidate();
        menuContainer.repaint();
    }

    public void selectItem(int navIndex, boolean notify) {
        if (navIndex < 0 || navIndex >= navItems.size()) return;
        NavItemView target = navItems.get(navIndex);
        if (!target.isEnabledState() || !target.isVisible()) return;

        this.selectedIndex = navIndex;
        for (NavItemView item : navItems) {
            item.setActive(item.navIndex == navIndex);
        }

        if (notify && listener != null) {
            listener.onNavigationSelected(navIndex);
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setItemEnabled(int navIndex, boolean enabled) {
        if (navIndex >= 0 && navIndex < navItems.size()) {
            navItems.get(navIndex).setEnabledState(enabled);
            if (!enabled && selectedIndex == navIndex) {
                // Pindah ke denah jika tab aktif dinonaktifkan
                selectItem(NAV_FLOOR_PLAN, true);
            }
        }
    }

    public boolean isItemEnabled(int navIndex) {
        if (navIndex >= 0 && navIndex < navItems.size()) {
            return navItems.get(navIndex).isEnabledState();
        }
        return false;
    }

    public void setItemVisible(int navIndex, boolean visible) {
        if (navIndex >= 0 && navIndex < navItems.size()) {
            navItems.get(navIndex).setVisible(visible);
            if (!visible && selectedIndex == navIndex) {
                selectItem(NAV_FLOOR_PLAN, true);
            }
            updateSectionVisibility();
        }
    }

    public boolean isItemVisible(int navIndex) {
        if (navIndex >= 0 && navIndex < navItems.size()) {
            return navItems.get(navIndex).isVisible();
        }
        return false;
    }

    public void setBadgeCount(int navIndex, int count) {
        if (navIndex >= 0 && navIndex < navItems.size()) {
            navItems.get(navIndex).setBadgeCount(count);
        }
    }

    /**
     * Terapkan visibilitas dan hak akses menu sidebar secara dinamis sesuai peran pengguna:
     * - Mahasiswa: Hanya melihat menu utama (Denah, Katalog, Pengajuan). Menu Admin & Sekdek disembunyikan total.
     * - Sekretaris: Melihat menu utama + portal dekanat/sekretaris. Menu Admin disembunyikan total.
     * - Administrator: Melihat seluruh 9 menu administrasi sistem lengkap.
     */
    public void applyRolePermissions(String role) {
        if (role == null) role = "MAHASISWA";
        this.currentRoleStr = role;
        String rUpper = role.trim().toUpperCase();

        if (rUpper.contains("MAHASISWA")) {
            // Menu utama pengguna tampil & aktif
            for (int i = 0; i <= NAV_BOOKING; i++) {
                setItemVisible(i, true);
                setItemEnabled(i, true);
            }
            // Menu sekretaris & admin disembunyikan total (tidak terlihat sama sekali)
            for (int i = NAV_SEC_VALIDATION; i <= NAV_LAYOUT_EDITOR; i++) {
                setItemVisible(i, false);
                setItemEnabled(i, false);
            }
            if (panelStudentInfo != null) {
                panelStudentInfo.setVisible(!isCollapsed);
            }
            if (selectedIndex > NAV_BOOKING) {
                selectItem(NAV_FLOOR_PLAN, true);
            }
        } else if (rUpper.contains("SEKDEK") || rUpper.contains("SEKRETARIS")) {
            // Menu utama & sekretaris tampil & aktif
            for (int i = 0; i <= NAV_SEC_SCHEDULE; i++) {
                setItemVisible(i, true);
                setItemEnabled(i, true);
            }
            // Menu master admin disembunyikan total
            for (int i = NAV_ADMIN_APPROVAL; i <= NAV_LAYOUT_EDITOR; i++) {
                setItemVisible(i, false);
                setItemEnabled(i, false);
            }
            if (panelStudentInfo != null) {
                panelStudentInfo.setVisible(false);
            }
            if (selectedIndex > NAV_SEC_SCHEDULE) {
                selectItem(NAV_SEC_VALIDATION, true);
            }
        } else if (rUpper.contains("ADMIN")) {
            // Admin master melihat seluruh modul
            for (int i = 0; i < navItems.size(); i++) {
                setItemVisible(i, true);
                setItemEnabled(i, true);
            }
            if (panelStudentInfo != null) {
                panelStudentInfo.setVisible(false);
            }
        } else {
            // Tamu / Guest
            setItemVisible(NAV_FLOOR_PLAN, true);
            setItemVisible(NAV_CATALOG, true);
            setItemVisible(NAV_BOOKING, false);
            setItemEnabled(NAV_FLOOR_PLAN, true);
            setItemEnabled(NAV_CATALOG, true);
            setItemEnabled(NAV_BOOKING, false);

            for (int i = NAV_SEC_VALIDATION; i < navItems.size(); i++) {
                setItemVisible(i, false);
                setItemEnabled(i, false);
            }
            if (panelStudentInfo != null) {
                panelStudentInfo.setVisible(!isCollapsed);
            }
            selectItem(NAV_FLOOR_PLAN, false);
        }
        updateSectionVisibility();
    }

    private void updateSectionVisibility() {
        if (panelSectionSecretary != null && navItems.size() > NAV_SEC_SCHEDULE) {
            boolean secVisible = navItems.get(NAV_SEC_VALIDATION).isVisible() || navItems.get(NAV_SEC_SCHEDULE).isVisible();
            panelSectionSecretary.setVisible(secVisible);
            if (spacerSecretary != null) spacerSecretary.setVisible(secVisible);
        }

        if (panelSectionAdmin != null && navItems.size() > NAV_LAYOUT_EDITOR) {
            boolean adminVisible = navItems.get(NAV_ADMIN_APPROVAL).isVisible()
                    || navItems.get(NAV_ADMIN_CAMPUS).isVisible()
                    || navItems.get(NAV_ADMIN_ROOMS).isVisible()
                    || navItems.get(NAV_LAYOUT_EDITOR).isVisible();
            panelSectionAdmin.setVisible(adminVisible);
            if (spacerAdmin != null) spacerAdmin.setVisible(adminVisible);
        }

        revalidate();
        repaint();
    }

    public void updateUserProfile(User user) {
        this.currentUser = user;
        if (user == null) {
            lblUserName.setText("Tamu");
            lblUserName.setToolTipText("Pengguna Tamu / Belum Login");
            lblUserRole.setText("GUEST");
            lblUserRole.setForeground(ModernUIHelper.TEXT_MUTED);
            avatarContainer.removeAll();
            avatarContainer.add(ModernUIHelper.createAvatarComponent("Tamu", "GUEST", 36), BorderLayout.CENTER);
            applyRolePermissions("GUEST");
            if (isCollapsed) {
                profileCard.setToolTipText("Tamu • Klik untuk menu akun");
                avatarContainer.setToolTipText("Tamu • Klik untuk menu akun");
            }
            avatarContainer.revalidate();
            avatarContainer.repaint();
            return;
        }

        lblUserName.setText(user.getFullName());
        lblUserName.setToolTipText(user.getFullName() + " (" + user.getRoleDisplayName() + ")");
        lblUserRole.setText(user.getRoleDisplayName().toUpperCase());

        String role = (user.getRole() != null) ? user.getRole().toUpperCase() : "MAHASISWA";
        if (role.contains("MAHASISWA")) {
            lblUserRole.setForeground(new Color(56, 189, 248)); // Sky Blue
        } else if (role.contains("SEKDEK") || role.contains("SEKRETARIS")) {
            lblUserRole.setForeground(new Color(192, 132, 252)); // Purple
        } else {
            lblUserRole.setForeground(new Color(52, 211, 153)); // Emerald
        }

        avatarContainer.removeAll();
        avatarContainer.add(ModernUIHelper.createAvatarComponent(user.getFullName(), user.getRole(), 36), BorderLayout.CENTER);
        applyRolePermissions(role);
        if (isCollapsed) {
            String tip = user.getFullName() + " (" + user.getRoleDisplayName() + ") • Klik untuk menu akun";
            profileCard.setToolTipText(tip);
            avatarContainer.setToolTipText(tip);
        }
        avatarContainer.revalidate();
        avatarContainer.repaint();
    }

    /**
     * Tampilan satu baris tombol item navigasi interaktif
     */
    private class NavItemView extends JPanel {
        private final int navIndex;
        private final ModernUIHelper.VectorIcon vectorIcon;
        private final String titleStr;
        private final String subStr;

        private boolean isActive = false;
        private boolean isHover = false;
        private boolean isEnabledState = true;
        private int badgeCount = 0;

        private final JLabel lblIcon;
        private final JLabel lblTitle;
        private final JLabel lblSub;
        private final JLabel lblBadge;
        private final JPanel leftBar;
        private final JPanel centerPanel;
        private final JPanel textCol;
        private final JPanel badgeWrapper;

        public NavItemView(int navIndex, ModernUIHelper.VectorIcon.Type iconType, String title, String subtitle) {
            this.navIndex = navIndex;
            this.vectorIcon = new ModernUIHelper.VectorIcon(iconType, 18);
            this.titleStr = title;
            this.subStr = subtitle;

            setLayout(new BorderLayout(8, 0));
            setOpaque(false);
            setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
            setPreferredSize(new Dimension(245, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(3, 4, 3, 8));

            // Left Active Indicator Bar
            leftBar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    if (isOpaque()) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(getBackground());
                        g2.fillRoundRect(0, 4, getWidth(), getHeight() - 8, 3, 3);
                        g2.dispose();
                    }
                }
            };
            leftBar.setPreferredSize(new Dimension(3, 32));
            leftBar.setOpaque(false);
            add(leftBar, BorderLayout.WEST);

            // Center Content: Icon + Texts
            centerPanel = new JPanel(new BorderLayout(8, 0));
            centerPanel.setOpaque(false);

            lblIcon = new JLabel(vectorIcon, JLabel.CENTER);
            lblIcon.setPreferredSize(new Dimension(26, 26));
            centerPanel.add(lblIcon, BorderLayout.WEST);

            textCol = new JPanel(new GridLayout(2, 1, 0, 1));
            textCol.setOpaque(false);

            lblTitle = new JLabel(title);
            lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblTitle.setForeground(new Color(203, 213, 225)); // Slate 300

            lblSub = new JLabel(subtitle);
            lblSub.setFont(new Font("SansSerif", Font.PLAIN, 9));
            lblSub.setForeground(new Color(148, 163, 184)); // Slate 400

            textCol.add(lblTitle);
            textCol.add(lblSub);
            centerPanel.add(textCol, BorderLayout.CENTER);
            add(centerPanel, BorderLayout.CENTER);

            // East: Badge pill (e.g. pending notification counter)
            lblBadge = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lblBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblBadge.setOpaque(false);
            lblBadge.setVisible(false);
            lblBadge.setHorizontalAlignment(SwingConstants.CENTER);
            lblBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

            badgeWrapper = new JPanel(new GridBagLayout());
            badgeWrapper.setOpaque(false);
            badgeWrapper.add(lblBadge);
            add(badgeWrapper, BorderLayout.EAST);

            // Forward Mouse Listener to all child components
            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabledState && isVisible() && !isActive) {
                        isHover = true;
                        updateVisualState();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (isEnabledState && isVisible() && !isActive) {
                        isHover = false;
                        updateVisualState();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabledState && isVisible()) {
                        selectItem(navIndex, true);
                    }
                }
            };

            addMouseListener(mouseHandler);
            centerPanel.addMouseListener(mouseHandler);
            lblIcon.addMouseListener(mouseHandler);
            textCol.addMouseListener(mouseHandler);
            lblTitle.addMouseListener(mouseHandler);
            lblSub.addMouseListener(mouseHandler);
        }

        public void setCollapsed(boolean collapsed) {
            if (collapsed) {
                setMaximumSize(new Dimension(56, 44));
                setPreferredSize(new Dimension(56, 44));
                setBorder(new EmptyBorder(3, 2, 3, 2));
                textCol.setVisible(false);
                badgeWrapper.setVisible(false);
                centerPanel.removeAll();
                centerPanel.setLayout(new GridBagLayout());
                centerPanel.add(lblIcon);
                setToolTipText(titleStr + (badgeCount > 0 ? " • (" + badgeCount + " Menunggu)" : ""));
            } else {
                setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
                setPreferredSize(new Dimension(245, 44));
                setBorder(new EmptyBorder(3, 4, 3, 8));
                textCol.setVisible(true);
                badgeWrapper.setVisible(badgeCount > 0);
                centerPanel.removeAll();
                centerPanel.setLayout(new BorderLayout(8, 0));
                centerPanel.add(lblIcon, BorderLayout.WEST);
                centerPanel.add(textCol, BorderLayout.CENTER);
                setToolTipText(isEnabledState ? null : "Akses terbatas untuk peran pengguna Anda");
            }
            updateVisualState();
        }

        public void setActive(boolean active) {
            this.isActive = active;
            this.isHover = false;
            updateVisualState();
        }

        public boolean isEnabledState() {
            return isEnabledState;
        }

        public void setEnabledState(boolean enabled) {
            this.isEnabledState = enabled;
            super.setEnabled(enabled);
            setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            if (isCollapsed) {
                setToolTipText(enabled ? (titleStr + (badgeCount > 0 ? " • (" + badgeCount + " Menunggu)" : "")) : (titleStr + " 🔒 Akses terbatas"));
            } else {
                if (!enabled) {
                    lblTitle.setText(titleStr + " 🔒");
                    setToolTipText("Akses terbatas untuk peran pengguna Anda");
                } else {
                    lblTitle.setText(titleStr);
                    setToolTipText(null);
                }
            }
            updateVisualState();
        }

        public void setBadgeCount(int count) {
            this.badgeCount = count;
            if (count > 0 && isEnabledState && isVisible()) {
                lblBadge.setText(String.valueOf(count));
                lblBadge.setBackground(ModernUIHelper.COLOR_DANGER);
                lblBadge.setForeground(Color.WHITE);
                lblBadge.setVisible(!isCollapsed);
            } else {
                lblBadge.setVisible(false);
            }
            if (isCollapsed) {
                setToolTipText(titleStr + (badgeCount > 0 ? " • (" + badgeCount + " Menunggu)" : ""));
            }
            revalidate();
            repaint();
        }

        private void updateVisualState() {
            if (!isEnabledState) {
                lblTitle.setForeground(new Color(71, 85, 105)); // Slate 600
                lblSub.setForeground(new Color(51, 65, 85));
                vectorIcon.setColor(new Color(71, 85, 105));
                leftBar.setOpaque(false);
            } else if (isActive) {
                lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
                lblTitle.setForeground(Color.WHITE);
                lblSub.setForeground(new Color(186, 230, 253)); // Sky 200
                vectorIcon.setColor(new Color(56, 189, 248)); // Sky 400
                leftBar.setOpaque(true);
                leftBar.setBackground(new Color(56, 189, 248));
            } else {
                lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lblTitle.setForeground(isHover ? Color.WHITE : new Color(203, 213, 225)); // Slate 300
                lblSub.setForeground(isHover ? new Color(226, 232, 240) : new Color(148, 163, 184));
                vectorIcon.setColor(isHover ? Color.WHITE : new Color(203, 213, 225));
                leftBar.setOpaque(false);
            }
            lblIcon.repaint();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!isEnabledState || !isVisible()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isActive) {
                g2.setColor(new Color(30, 41, 59)); // Slate 800
                g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);
                g2.setColor(new Color(59, 130, 246, 110)); // Soft accent border
                g2.drawRoundRect(2, 2, w - 4, h - 4, 8, 8);
            } else if (isHover) {
                g2.setColor(new Color(255, 255, 255, 12)); // Translucent hover
                g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);
            }

            g2.dispose();
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (isCollapsed && badgeCount > 0 && isEnabledState && isVisible()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int dotSize = 8;
                int dotX = getWidth() - 15;
                int dotY = 7;
                g2.setColor(ModernUIHelper.COLOR_DANGER);
                g2.fillOval(dotX, dotY, dotSize, dotSize);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(dotX, dotY, dotSize, dotSize);
                g2.dispose();
            }
        }
    }
}
