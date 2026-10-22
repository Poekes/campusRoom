package com.campusroom.ui;

import com.campusroom.dao.BookingDao;
import com.campusroom.dao.BuildingDao;
import com.campusroom.dao.CampusDao;
import com.campusroom.dao.FloorPlanDao;
import com.campusroom.dao.RoomDao;
import com.campusroom.model.Building;
import com.campusroom.model.Campus;
import com.campusroom.model.Room;
import com.campusroom.model.User;
import com.campusroom.service.AuthService;
import com.campusroom.service.AvailabilityService;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * MainFrame - Jendela Utama Aplikasi CampusRoom dengan Navigasi Sidebar Interaktif Modern.
 * Desain bersih, minimalis, dan elegan bergaya SaaS enterprise.
 * Menghubungkan 9 menu utama:
 * 1. Denah Visual Interaktif & Realtime Filter
 * 2. Katalog & Pencarian Ruangan
 * 3. Riwayat & Pengajuan Booking Mahasiswa
 * 4. Validasi Jadwal Mahasiswa (Sekdek)
 * 5. Master Jadwal Tetap Perkuliahan (Sekdek)
 * 6. Approval Akhir Reservasi (Admin Master)
 * 7. Multi-Kampus & Gedung (Admin Master)
 * 8. Kelola Master Ruangan (Admin Master)
 * 9. Visual Ribbon Layout Editor Denah (Admin Master)
 */
public class MainFrame extends JFrame implements InteractiveFloorPlanPanel.FloorPlanListener, AuthService.AuthListener, SidebarNavigationPanel.NavigationListener {
    private final CampusDao campusDao = new CampusDao();
    private final BuildingDao buildingDao = new BuildingDao();
    private final RoomDao roomDao = new RoomDao();
    private final FloorPlanDao floorPlanDao = new FloorPlanDao();
    private final BookingDao bookingDao = new BookingDao();
    private final AvailabilityService availabilityService = new AvailabilityService();
    private boolean isUpdatingFloorCombo = false;

    // Interactive Sidebar
    private SidebarNavigationPanel sidebarNav;

    // Minimalist Top App Bar
    private JButton btnToggleSidebar;
    private JLabel lblBreadcrumbView;
    private JLabel lblViewSubtitle;
    private JLabel lblRoleBadge;
    private JLabel lblDateChip;

    // Filter Controls (Scoped to Floor Plan Tab)
    private JComboBox<Campus> cbCampus;
    private JComboBox<Building> cbBuilding;
    private JComboBox<String> cbFloor;
    private JTextField txtDate;
    private JComboBox<String> cbSlot;
    private JButton btnRefreshStatus;

    // Card Layout Views
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private InteractiveFloorPlanPanel floorCanvas;
    private RoomCatalogPanel catalogPanel;
    private StudentBookingPanel studentBookingPanel;
    private SecretaryPanel secretaryPanel;
    private AdminPanel adminPanel;
    private LayoutEditorPanel layoutEditorPanel;

    // Sidebar Info in Denah Tab
    private JPanel roomDetailSidebar;
    private JLabel lblSidebarRoomTitle;
    private JLabel lblSidebarStatusBanner;
    private JLabel lblSidebarCapacity;
    private JLabel lblSidebarFloor;
    private JLabel lblSidebarFacilities;
    private JLabel lblSidebarOccupant;
    private JButton btnSidebarBook;

    // Floor Plan Stats and Legend (Scoped to Floor Plan)
    private JLabel lblFloorStats;

    // Global Bottom Status Bar
    private JLabel lblGlobalDbStatus;
    private JLabel lblGlobalUserContext;

    private List<Room> currentRooms;
    private Room selectedRoom;

    public MainFrame() {
        setTitle("CampusRoom — Aplikasi Manajemen & Reservasi Ruangan Kampus");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380, 860);
        setMinimumSize(new Dimension(1120, 720));
        setLocationRelativeTo(null);

        AuthService.getInstance().addListener(this);

        initUI();
        loadCampuses();
        refreshAvailability();
    }

    private void initUI() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(ModernUIHelper.BG_PAGE);

        // 1. West: Interactive Sidebar Navigation
        sidebarNav = new SidebarNavigationPanel(this);
        getContentPane().add(sidebarNav, BorderLayout.WEST);

        // 2. Center: Main Application Canvas Area
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setOpaque(false);

        // Top App Bar (Clean, Minimalist Breadcrumb & Role Context)
        mainContentPanel.add(createTopAppBar(), BorderLayout.NORTH);

        // Center Views in CardLayout
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(ModernUIHelper.BG_PAGE);

        // View 0: Denah Interaktif (dengan filter card & inspector internal)
        cardPanel.add(createFloorPlanTab(), "floorPlan");

        // View 1: Katalog Ruangan
        catalogPanel = new RoomCatalogPanel(new RoomCatalogPanel.TableCallback() {
            @Override
            public void onBookRoom(Room room) {
                openBookingDialog(room);
            }

            @Override
            public void onViewDetail(Room room) {
                openDetailDialog(room);
            }
        });
        cardPanel.add(catalogPanel, "catalog");

        // View 2: Pengajuan Mahasiswa
        studentBookingPanel = new StudentBookingPanel(() -> {
            if (selectedRoom != null && selectedRoom.isAvailable()) {
                openBookingDialog(selectedRoom);
            } else {
                switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);
                JOptionPane.showMessageDialog(this,
                        "Silakan pilih salah satu ruangan yang kosong (warna hijau) pada denah terlebih dahulu!",
                        "Pilih Ruangan", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        cardPanel.add(studentBookingPanel, "booking");

        // View 3: Validasi Pengajuan (Sekdek)
        secretaryPanel = new SecretaryPanel(() -> refreshAvailability());
        cardPanel.add(secretaryPanel.getValidationPanel(), "secValidation");

        // View 4: Jadwal Tetap Perkuliahan (Sekdek)
        cardPanel.add(secretaryPanel.getSchedulePanel(), "secSchedule");

        // View 5, 6, 7: Portal Admin Master
        adminPanel = new AdminPanel(() -> {
            loadCampuses();
            if (layoutEditorPanel != null) {
                layoutEditorPanel.loadCampuses();
            }
            refreshAvailability();
        });
        cardPanel.add(adminPanel.getApprovalPanel(), "adminApproval");
        cardPanel.add(adminPanel.getCampusBuildingPanel(), "adminCampus");
        cardPanel.add(adminPanel.getRoomManagementPanel(), "adminRooms");

        // View 8: Visual Layout Editor
        layoutEditorPanel = new LayoutEditorPanel(() -> refreshAvailability());
        cardPanel.add(layoutEditorPanel, "layoutEditor");

        mainContentPanel.add(cardPanel, BorderLayout.CENTER);

        // Bottom Global Status Bar
        mainContentPanel.add(createBottomStatusBar(), BorderLayout.SOUTH);

        getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        updateRoleView(AuthService.getInstance().getCurrentUser());

        // Keyboard shortcut: Ctrl + B (atau Cmd + B) untuk buka / ciutkan sidebar
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "toggleSidebar"
        );
        getRootPane().getActionMap().put("toggleSidebar", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (sidebarNav != null) {
                    sidebarNav.toggleCollapse();
                }
            }
        });
    }

    /**
     * Top Bar Minimalis bergaya SaaS modern:
     * Menampilkan breadcrumb modul aktif, subtitle, tanggal hari ini, dan chip peran.
     * Tidak ada filter raksasa di sini sehingga bebas tabrakan layout pada resolusi apa pun.
     */
    private JPanel createTopAppBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernUIHelper.BORDER_LIGHT),
                new EmptyBorder(10, 20, 10, 20)
        ));

        // Left: Sidebar Toggle Button + Breadcrumb & Title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        btnToggleSidebar = new JButton("☰") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(226, 232, 240));
                } else {
                    g2.setColor(new Color(241, 245, 249));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnToggleSidebar.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnToggleSidebar.setForeground(ModernUIHelper.TEXT_DARK);
        btnToggleSidebar.setFocusPainted(false);
        btnToggleSidebar.setBorderPainted(false);
        btnToggleSidebar.setContentAreaFilled(false);
        btnToggleSidebar.setOpaque(false);
        btnToggleSidebar.setPreferredSize(new Dimension(32, 32));
        btnToggleSidebar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleSidebar.setToolTipText(sidebarNav != null && sidebarNav.isCollapsed() ? "Perluas Sidebar (Ctrl+B)" : "Ciutkan Sidebar (Ctrl+B)");
        btnToggleSidebar.addActionListener(e -> {
            if (sidebarNav != null) {
                sidebarNav.toggleCollapse();
            }
        });
        leftPanel.add(btnToggleSidebar);

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);

        lblBreadcrumbView = new JLabel("CampusRoom / Denah Visual Interaktif");
        lblBreadcrumbView.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblBreadcrumbView.setForeground(ModernUIHelper.TEXT_DARK);

        lblViewSubtitle = new JLabel("Peta visual ruangan realtime dan status ketersediaan per slot sesi kuliah");
        lblViewSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblViewSubtitle.setForeground(ModernUIHelper.TEXT_SECONDARY);

        titleCol.add(lblBreadcrumbView);
        titleCol.add(Box.createVerticalStrut(2));
        titleCol.add(lblViewSubtitle);
        leftPanel.add(titleCol);
        bar.add(leftPanel, BorderLayout.WEST);

        // Right: Date & Role Context Controls
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        lblDateChip = new JLabel(formatIndonesianDate(LocalDate.now())) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(ModernUIHelper.BORDER_LIGHT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblDateChip.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblDateChip.setOpaque(false);
        lblDateChip.setBackground(new Color(248, 250, 252));
        lblDateChip.setForeground(ModernUIHelper.TEXT_DARK);
        lblDateChip.setBorder(new EmptyBorder(4, 10, 4, 10));

        lblRoleBadge = new JLabel("MAHASISWA") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblRoleBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblRoleBadge.setOpaque(false);
        lblRoleBadge.setBackground(new Color(224, 242, 254)); // Sky 100
        lblRoleBadge.setForeground(new Color(2, 132, 199));   // Sky 600
        lblRoleBadge.setBorder(new EmptyBorder(4, 10, 4, 10));

        JButton btnQuickSwitch = ModernUIHelper.createStyledButton("Ganti Akun", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnQuickSwitch.addActionListener(e -> showRoleSwitchMenu());

        rightPanel.add(lblDateChip);
        rightPanel.add(lblRoleBadge);
        rightPanel.add(btnQuickSwitch);
        bar.add(rightPanel, BorderLayout.EAST);

        return bar;
    }

    private String formatIndonesianDate(LocalDate date) {
        String[] days = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
        String[] months = {"Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des"};
        int dayIdx = date.getDayOfWeek().getValue() % 7;
        return days[dayIdx] + ", " + date.getDayOfMonth() + " " + months[date.getMonthValue() - 1] + " " + date.getYear();
    }

    /**
     * Tampilan Denah Visual: Menyertakan Card Filter khusus di atas canvas denah,
     * canvas denah di tengah, dan inspector detail ruangan di kanan.
     */
    private JPanel createFloorPlanTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        panel.setBackground(ModernUIHelper.BG_PAGE);

        // 1. North: Modern Filter Toolbar Card khusus denah
        panel.add(createFloorPlanFilterCard(), BorderLayout.NORTH);

        // 2. Center: Canvas Denah
        floorCanvas = new InteractiveFloorPlanPanel();
        floorCanvas.setFloorPlanListener(this);
        JScrollPane canvasScroll = new JScrollPane(floorCanvas);
        canvasScroll.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));
        panel.add(canvasScroll, BorderLayout.CENTER);

        // 3. East: Detail Ruangan Inspector
        panel.add(createRoomDetailSidebar(), BorderLayout.EAST);

        return panel;
    }

    private JPanel createFloorPlanFilterCard() {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        // Baris 1: Filter Lokasi (Kampus, Gedung, Lantai) di Kiri, Tombol Aksi di Kanan
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        JPanel locationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        locationPanel.setOpaque(false);

        JLabel lblC = new JLabel("Kampus:");
        lblC.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblC.setForeground(new Color(71, 85, 105));
        locationPanel.add(lblC);

        cbCampus = new JComboBox<>();
        cbCampus.setPreferredSize(new Dimension(135, 28));
        cbCampus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cbCampus.addActionListener(e -> onCampusSelected());
        locationPanel.add(cbCampus);

        JLabel lblB = new JLabel("Gedung:");
        lblB.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblB.setForeground(new Color(71, 85, 105));
        locationPanel.add(lblB);

        cbBuilding = new JComboBox<>();
        cbBuilding.setPreferredSize(new Dimension(135, 28));
        cbBuilding.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cbBuilding.addActionListener(e -> onBuildingSelected());
        locationPanel.add(cbBuilding);

        JLabel lblF = new JLabel("Lantai:");
        lblF.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblF.setForeground(new Color(71, 85, 105));
        locationPanel.add(lblF);

        cbFloor = new JComboBox<>();
        cbFloor.setPreferredSize(new Dimension(85, 28));
        cbFloor.setFont(new Font("SansSerif", Font.BOLD, 11));
        cbFloor.addActionListener(e -> {
            if (!isUpdatingFloorCombo) refreshAvailability();
        });
        locationPanel.add(cbFloor);

        row1.add(locationPanel, BorderLayout.WEST);

        // Kanan Baris 1: Tombol Cek Jadwal Realtime
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionPanel.setOpaque(false);

        btnRefreshStatus = ModernUIHelper.createStyledButton("Cek Jadwal", ModernUIHelper.COLOR_PRIMARY, Color.WHITE, 11, true);
        btnRefreshStatus.setPreferredSize(new Dimension(105, 28));
        btnRefreshStatus.addActionListener(e -> refreshAvailability());
        actionPanel.add(btnRefreshStatus);

        row1.add(actionPanel, BorderLayout.EAST);
        card.add(row1);

        // Baris 2: Filter Waktu (Tanggal, Sesi) di Kiri, Legenda & Statistik Denah di Kanan
        JPanel row2 = new JPanel(new BorderLayout(10, 0));
        row2.setOpaque(false);

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        timePanel.setOpaque(false);

        JLabel lblD = new JLabel("Tanggal:");
        lblD.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblD.setForeground(new Color(71, 85, 105));
        timePanel.add(lblD);

        txtDate = new JTextField(LocalDate.now().toString(), 8);
        txtDate.setFont(new Font("SansSerif", Font.PLAIN, 11));
        txtDate.setPreferredSize(new Dimension(85, 28));
        timePanel.add(txtDate);

        JLabel lblS = new JLabel("Sesi Kuliah:");
        lblS.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblS.setForeground(new Color(71, 85, 105));
        timePanel.add(lblS);

        String[] slots = {
                "08:00 - 10:30 (Sesi Pagi 1)",
                "10:30 - 13:00 (Sesi Siang 1)",
                "13:00 - 15:30 (Sesi Siang 2)",
                "15:30 - 18:00 (Sesi Sore)",
                "18:00 - 20:30 (Sesi Malam)"
        };
        cbSlot = new JComboBox<>(slots);
        cbSlot.setPreferredSize(new Dimension(150, 28));
        cbSlot.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cbSlot.addActionListener(e -> refreshAvailability());
        timePanel.add(cbSlot);

        row2.add(timePanel, BorderLayout.WEST);

        // Kanan Baris 2: Legenda & Stats dengan Pill Badges Modern
        JPanel legendStatsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        legendStatsPanel.setOpaque(false);

        JLabel leg1 = createPillBadge("Kosong", new Color(16, 185, 129), new Color(220, 252, 231), new Color(22, 101, 52));
        JLabel leg2 = createPillBadge("Terpakai", new Color(239, 68, 68), new Color(254, 226, 226), new Color(153, 27, 27));
        JLabel leg3 = createPillBadge("Terpilih", new Color(245, 158, 11), new Color(254, 243, 199), new Color(180, 83, 9));

        lblFloorStats = createPillBadge("Total Lt.2: 8 | Kosong: 8 | Terpakai: 0", null, new Color(241, 245, 249), ModernUIHelper.COLOR_PRIMARY);

        legendStatsPanel.add(leg1);
        legendStatsPanel.add(leg2);
        legendStatsPanel.add(leg3);
        legendStatsPanel.add(lblFloorStats);

        row2.add(legendStatsPanel, BorderLayout.EAST);
        card.add(row2);

        return card;
    }

    private JLabel createPillBadge(String text, Color dotColor, Color bg, Color fg) {
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                if (dotColor != null) {
                    g2.setColor(dotColor);
                    int dotSize = 6;
                    int dotY = (getHeight() - dotSize) / 2;
                    g2.fillOval(8, dotY, dotSize, dotSize);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setOpaque(false);
        lbl.setBackground(bg);
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(3, dotColor != null ? 18 : 8, 3, 8));
        return lbl;
    }

    private JPanel createRoomDetailSidebar() {
        roomDetailSidebar = new JPanel(new BorderLayout(10, 12));
        roomDetailSidebar.setPreferredSize(new Dimension(285, 500));
        roomDetailSidebar.setBackground(Color.WHITE);
        roomDetailSidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT, 1),
                new EmptyBorder(16, 16, 16, 16)
        ));

        // Header: Badge info + Title + Status Banner
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);

        JLabel lblBadge = new JLabel("INSPECTOR RUANGAN");
        lblBadge.setFont(new Font("SansSerif", Font.BOLD, 9));
        lblBadge.setForeground(new Color(100, 116, 139));
        top.add(lblBadge, BorderLayout.NORTH);

        JPanel titleBannerCol = new JPanel(new GridLayout(2, 1, 0, 6));
        titleBannerCol.setOpaque(false);

        lblSidebarRoomTitle = new JLabel("Pilih Ruangan di Denah", JLabel.LEFT);
        lblSidebarRoomTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSidebarRoomTitle.setForeground(ModernUIHelper.TEXT_DARK);

        lblSidebarStatusBanner = new JLabel("Klik salah satu kotak ruangan di denah", JLabel.CENTER) {
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
        lblSidebarStatusBanner.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblSidebarStatusBanner.setOpaque(false);
        lblSidebarStatusBanner.setBackground(new Color(241, 245, 249));
        lblSidebarStatusBanner.setForeground(new Color(100, 116, 139));
        lblSidebarStatusBanner.setBorder(new EmptyBorder(6, 10, 6, 10));

        titleBannerCol.add(lblSidebarRoomTitle);
        titleBannerCol.add(lblSidebarStatusBanner);
        top.add(titleBannerCol, BorderLayout.CENTER);
        roomDetailSidebar.add(top, BorderLayout.NORTH);

        // Body: Specification Card
        JPanel cardSpec = new JPanel(new GridLayout(4, 1, 0, 8));
        cardSpec.setBackground(new Color(248, 250, 252));
        cardSpec.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        lblSidebarCapacity = new JLabel("<html><b style='color:#64748b; font-size:9px;'>KAPASITAS</b><br/><span style='color:#334155;'>-</span></html>");
        lblSidebarFloor = new JLabel("<html><b style='color:#64748b; font-size:9px;'>LOKASI LANTAI</b><br/><span style='color:#334155;'>-</span></html>");
        lblSidebarFacilities = new JLabel("<html><b style='color:#64748b; font-size:9px;'>FASILITAS</b><br/><span style='color:#334155;'>-</span></html>");
        lblSidebarOccupant = new JLabel("<html><b style='color:#64748b; font-size:9px;'>STATUS PENGGUNAAN</b><br/><span style='color:#334155;'>-</span></html>");

        cardSpec.add(lblSidebarCapacity);
        cardSpec.add(lblSidebarFloor);
        cardSpec.add(lblSidebarFacilities);
        cardSpec.add(lblSidebarOccupant);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(cardSpec, BorderLayout.CENTER);

        JLabel lblHint = new JLabel("<html><i>* Klik 2x pada kotak denah untuk melihat informasi lengkap.</i></html>");
        lblHint.setFont(new Font("SansSerif", Font.ITALIC, 10));
        lblHint.setForeground(new Color(148, 163, 184));
        lblHint.setBorder(new EmptyBorder(4, 2, 0, 2));
        centerPanel.add(lblHint, BorderLayout.SOUTH);

        roomDetailSidebar.add(centerPanel, BorderLayout.CENTER);

        // Action Buttons
        JPanel bottom = new JPanel(new GridLayout(2, 1, 8, 8));
        bottom.setOpaque(false);

        btnSidebarBook = ModernUIHelper.createStyledButton("Ajukan Booking Ruangan ➔", ModernUIHelper.COLOR_SUCCESS, Color.WHITE, 11, true);
        btnSidebarBook.setEnabled(false);
        btnSidebarBook.addActionListener(e -> {
            if (selectedRoom != null) {
                openBookingDialog(selectedRoom);
            }
        });

        JButton btnDetail = ModernUIHelper.createStyledButton("Detail & Jadwal Lengkap", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnDetail.addActionListener(e -> {
            if (selectedRoom != null) {
                openDetailDialog(selectedRoom);
            }
        });

        bottom.add(btnSidebarBook);
        bottom.add(btnDetail);
        roomDetailSidebar.add(bottom, BorderLayout.SOUTH);

        return roomDetailSidebar;
    }

    private JPanel createBottomStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernUIHelper.BORDER_LIGHT),
                new EmptyBorder(6, 18, 6, 18)
        ));

        lblGlobalDbStatus = new JLabel("Status Layanan: Terhubung • Sistem: Online & Stabil") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 185, 129));
                g2.fillOval(2, (getHeight() - 7) / 2, 7, 7);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblGlobalDbStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblGlobalDbStatus.setForeground(new Color(71, 85, 105));
        lblGlobalDbStatus.setBorder(new EmptyBorder(0, 16, 0, 0));
        statusPanel.add(lblGlobalDbStatus, BorderLayout.WEST);

        lblGlobalUserContext = new JLabel("CampusRoom PRO 1.0 • Peran Aktif: Mahasiswa");
        lblGlobalUserContext.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblGlobalUserContext.setForeground(new Color(100, 116, 139));
        statusPanel.add(lblGlobalUserContext, BorderLayout.EAST);

        return statusPanel;
    }

    public void switchToView(int navIndex) {
        // Guard unauthorized navigation when a restricted role is active
        User currentUser = AuthService.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (navIndex > 1) {
                navIndex = SidebarNavigationPanel.NAV_FLOOR_PLAN;
            }
        } else {
            String role = (currentUser.getRole() != null) ? currentUser.getRole().toUpperCase() : "MAHASISWA";
            if (role.contains("MAHASISWA") && navIndex > 2) {
                navIndex = SidebarNavigationPanel.NAV_FLOOR_PLAN;
            } else if ((role.contains("SEKDEK") || role.contains("SEKRETARIS")) && navIndex > 4) {
                navIndex = SidebarNavigationPanel.NAV_SEC_VALIDATION;
            }
        }

        if (sidebarNav != null) {
            sidebarNav.selectItem(navIndex, false);
        }

        switch (navIndex) {
            case SidebarNavigationPanel.NAV_FLOOR_PLAN:
                cardLayout.show(cardPanel, "floorPlan");
                lblBreadcrumbView.setText("Denah Visual Interaktif");
                lblViewSubtitle.setText("Peta visual ruangan realtime dan status ketersediaan per slot sesi kuliah");
                break;
            case SidebarNavigationPanel.NAV_CATALOG:
                cardLayout.show(cardPanel, "catalog");
                lblBreadcrumbView.setText("Katalog Ruangan");
                lblViewSubtitle.setText("Daftar lengkap seluruh inventaris ruangan kuliah, kapasitas, fasilitas, dan ketersediaan");
                break;
            case SidebarNavigationPanel.NAV_BOOKING:
                cardLayout.show(cardPanel, "booking");
                lblBreadcrumbView.setText("Pengajuan Saya");
                lblViewSubtitle.setText("Portal permohonan peminjaman ruangan kuliah pengganti dan pelacakan status approval");
                if (studentBookingPanel != null) studentBookingPanel.refreshData();
                break;
            case SidebarNavigationPanel.NAV_SEC_VALIDATION:
                cardLayout.show(cardPanel, "secValidation");
                lblBreadcrumbView.setText("Portal Dekanat / Validasi Pengajuan");
                lblViewSubtitle.setText("Verifikasi jadwal perkuliahan pengganti mahasiswa terhadap master jadwal perkuliahan tetap");
                if (secretaryPanel != null) secretaryPanel.refreshData();
                break;
            case SidebarNavigationPanel.NAV_SEC_SCHEDULE:
                cardLayout.show(cardPanel, "secSchedule");
                lblBreadcrumbView.setText("Portal Dekanat / Master Jadwal Perkuliahan");
                lblViewSubtitle.setText("Kelola data jadwal tetap perkuliahan semester untuk mengunci ruangan kuliah secara rutin");
                if (secretaryPanel != null) secretaryPanel.refreshData();
                break;
            case SidebarNavigationPanel.NAV_ADMIN_APPROVAL:
                cardLayout.show(cardPanel, "adminApproval");
                lblBreadcrumbView.setText("Administrator / Approval Akhir");
                lblViewSubtitle.setText("Persetujuan final reservasi ruangan kuliah dan penetapan alokasi resmi");
                if (adminPanel != null) adminPanel.refreshData();
                break;
            case SidebarNavigationPanel.NAV_ADMIN_CAMPUS:
                cardLayout.show(cardPanel, "adminCampus");
                lblBreadcrumbView.setText("Administrator / Kelola Kampus & Gedung");
                lblViewSubtitle.setText("Konfigurasi seluruh lokasi kampus cabang dan gedung bertingkat");
                if (adminPanel != null) adminPanel.refreshData();
                break;
            case SidebarNavigationPanel.NAV_ADMIN_ROOMS:
                cardLayout.show(cardPanel, "adminRooms");
                lblBreadcrumbView.setText("Administrator / Master Ruangan");
                lblViewSubtitle.setText("Inventaris seluruh ruangan kuliah, kapasitas kursi, tipe ruangan, dan fasilitas");
                if (adminPanel != null) adminPanel.refreshData();
                break;
            case SidebarNavigationPanel.NAV_LAYOUT_EDITOR:
                cardLayout.show(cardPanel, "layoutEditor");
                lblBreadcrumbView.setText("Administrator / Desainer Denah Visual");
                lblViewSubtitle.setText("Editor tata letak denah interaktif (Toolbar Excalidraw & Inspector Modal)");
                break;
        }
    }

    @Override
    public void onNavigationSelected(int navIndex) {
        switchToView(navIndex);
    }

    @Override
    public void onSwitchRoleRequested() {
        showRoleSwitchMenu();
    }

    @Override
    public void onLogoutRequested() {
        handleLogout();
    }

    @Override
    public void onSidebarCollapseToggled(boolean isCollapsed) {
        if (btnToggleSidebar != null) {
            btnToggleSidebar.setToolTipText(isCollapsed ? "Perluas Sidebar (Ctrl+B)" : "Ciutkan Sidebar (Ctrl+B)");
        }
        revalidate();
        repaint();
    }

    private void loadCampuses() {
        cbCampus.removeAllItems();
        List<Campus> campuses = campusDao.getAllCampuses();
        for (Campus c : campuses) {
            cbCampus.addItem(c);
        }
        if (!campuses.isEmpty()) {
            cbCampus.setSelectedIndex(0);
            onCampusSelected();
        }
    }

    private void onCampusSelected() {
        Campus c = (Campus) cbCampus.getSelectedItem();
        if (c == null) return;
        cbBuilding.removeAllItems();
        List<Building> buildings = buildingDao.getBuildingsByCampusId(c.getId());
        for (Building b : buildings) {
            cbBuilding.addItem(b);
        }
        if (!buildings.isEmpty()) {
            cbBuilding.setSelectedIndex(0);
            onBuildingSelected();
        }
    }

    private void onBuildingSelected() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;

        isUpdatingFloorCombo = true;
        cbFloor.removeAllItems();
        int totalFloors = Math.max(1, b.getTotalFloors());
        for (int i = 1; i <= totalFloors; i++) {
            cbFloor.addItem("Lantai " + i);
        }

        // Default pilih Lantai 2 untuk Gedung A karena ruangan awal banyak di Lt 2
        if (b.getId() == 1 && totalFloors >= 2) {
            cbFloor.setSelectedIndex(1);
        } else {
            cbFloor.setSelectedIndex(0);
        }
        isUpdatingFloorCombo = false;

        refreshAvailability();
    }

    private int getSelectedFloorNumber() {
        String item = (String) cbFloor.getSelectedItem();
        if (item != null && item.startsWith("Lantai ")) {
            try {
                return Integer.parseInt(item.substring("Lantai ".length()).trim());
            } catch (Exception ignored) {
            }
        }
        return 1;
    }

    public void refreshAvailability() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;

        int floorNum = getSelectedFloorNumber();

        // 1. Muat Blueprint Gambar Denah untuk Gedung & Lantai Terpilih
        String imgPath = floorPlanDao.getFloorPlanImage(b.getId(), floorNum);
        floorCanvas.loadFloorImage(imgPath);

        // 2. Muat Ruangan khusus untuk Gedung & Lantai Terpilih
        currentRooms = roomDao.getRoomsByBuildingAndFloor(b.getId(), floorNum);

        LocalDate date;
        try {
            date = LocalDate.parse(txtDate.getText().trim());
        } catch (DateTimeParseException e) {
            date = LocalDate.now();
            txtDate.setText(date.toString());
        }

        String slotStr = (String) cbSlot.getSelectedItem();
        String startTime = "08:00";
        String endTime = "10:30";
        if (slotStr != null && slotStr.contains("-")) {
            String clean = slotStr.substring(0, slotStr.indexOf("(")).trim();
            String[] parts = clean.split("-");
            startTime = parts[0].trim();
            endTime = parts[1].trim();
        }

        // Hitung ketersediaan dinamis: Hijau vs Merah
        availabilityService.updateRoomsAvailability(currentRooms, date, startTime, endTime);

        // Update Floor Plan Canvas
        floorCanvas.setRooms(currentRooms);

        // Update Catalog Tab dengan seluruh ruangan di gedung ini beserta status ketersediaannya
        List<Room> allBuildingRooms = roomDao.getRoomsByBuildingId(b.getId());
        availabilityService.updateRoomsAvailability(allBuildingRooms, date, startTime, endTime);
        catalogPanel.setRooms(allBuildingRooms);

        // Hitung statistik ruangan denah
        int total = currentRooms.size();
        int avail = 0;
        for (Room r : currentRooms) {
            if (r.isAvailable()) avail++;
        }
        int occupied = total - avail;
        int rate = (total > 0) ? (avail * 100 / total) : 0;

        if (lblFloorStats != null) {
            lblFloorStats.setText("Total Lt." + floorNum + ": " + total + " | Kosong: " + avail + " | Terpakai: " + occupied);
        }

        // Refresh selected room detail if still exists
        if (selectedRoom != null) {
            Room matching = null;
            for (Room r : currentRooms) {
                if (r.getId() == selectedRoom.getId()) {
                    matching = r;
                    break;
                }
            }
            if (matching != null) {
                onRoomSelected(matching);
                floorCanvas.setSelectedRoom(matching);
            } else {
                resetSidebarInfo();
                floorCanvas.setSelectedRoom(null);
            }
        } else {
            resetSidebarInfo();
            floorCanvas.setSelectedRoom(null);
        }

        // Update badge counter di sidebar interaktif
        if (sidebarNav != null) {
            try {
                int pendingSekdek = bookingDao.getBookingsByStatus("MENUNGGU_SEKDEK").size();
                sidebarNav.setBadgeCount(SidebarNavigationPanel.NAV_SEC_VALIDATION, pendingSekdek);

                int pendingAdmin = bookingDao.getBookingsByStatus("DIVALIDASI_SEKDEK").size();
                sidebarNav.setBadgeCount(SidebarNavigationPanel.NAV_ADMIN_APPROVAL, pendingAdmin);
            } catch (Exception ignored) {
            }
        }

        // Refresh data di panel lain
        if (studentBookingPanel != null) studentBookingPanel.refreshData();
        if (secretaryPanel != null) secretaryPanel.refreshData();
        if (adminPanel != null) adminPanel.refreshData();
    }

    private void resetSidebarInfo() {
        selectedRoom = null;
        lblSidebarRoomTitle.setText("Pilih Ruangan di Denah");
        lblSidebarStatusBanner.setText("Klik salah satu kotak ruangan di denah");
        lblSidebarStatusBanner.setBackground(new Color(241, 245, 249));
        lblSidebarStatusBanner.setForeground(new Color(100, 116, 139));
        lblSidebarCapacity.setText("<html><b style='color:#64748b; font-size:9px;'>KAPASITAS</b><br/><span style='color:#94a3b8;'>-</span></html>");
        lblSidebarFloor.setText("<html><b style='color:#64748b; font-size:9px;'>LOKASI LANTAI</b><br/><span style='color:#94a3b8;'>-</span></html>");
        lblSidebarFacilities.setText("<html><b style='color:#64748b; font-size:9px;'>FASILITAS</b><br/><span style='color:#94a3b8;'>-</span></html>");
        lblSidebarOccupant.setText("<html><b style='color:#64748b; font-size:9px;'>STATUS PENGGUNAAN</b><br/><span style='color:#94a3b8;'>-</span></html>");
        btnSidebarBook.setEnabled(false);
        btnSidebarBook.setText("Ajukan Booking Ruangan");
        btnSidebarBook.setBackground(ModernUIHelper.COLOR_SUCCESS);
    }

    private void showRoleSwitchMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem item1 = new JMenuItem("Mahasiswa / PJ Kuliah (Muhamad Nur Akbar)");
        item1.addActionListener(e -> AuthService.getInstance().switchUserById(1));

        JMenuItem item2 = new JMenuItem("Sekretaris Dekan (Ibu Nurul Hidayah)");
        item2.addActionListener(e -> AuthService.getInstance().switchUserById(2));

        JMenuItem item3 = new JMenuItem("Administrator Master (Bpk. Rahmat Santoso)");
        item3.addActionListener(e -> AuthService.getInstance().switchUserById(3));

        menu.add(item1);
        menu.add(item2);
        menu.add(item3);
        int x = (sidebarNav != null && sidebarNav.isCollapsed()) ? (sidebarNav.getWidth() + 4) : 15;
        menu.show(sidebarNav, x, Math.max(10, sidebarNav.getHeight() - 140));
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Apakah Anda yakin ingin keluar (logout) dari sistem CampusRoom?",
                "Konfirmasi Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            AuthService.getInstance().logout();
            this.dispose();

            SwingUtilities.invokeLater(() -> {
                LoginDialog loginDialog = new LoginDialog(null, user -> {
                    MainFrame frame = new MainFrame();
                    frame.setVisible(true);
                });
                loginDialog.setVisible(true);
            });
        }
    }

    @Override
    public void onUserChanged(User newUser) {
        updateRoleView(newUser);
        refreshAvailability();
    }

    private void updateRoleView(User user) {
        if (sidebarNav == null) return;
        sidebarNav.updateUserProfile(user);

        if (user == null) {
            lblRoleBadge.setText("GUEST");
            lblRoleBadge.setBackground(new Color(241, 245, 249));
            lblRoleBadge.setForeground(ModernUIHelper.TEXT_SECONDARY);
            lblGlobalUserContext.setText("CampusRoom PRO 1.0 • Peran Aktif: Guest");
            sidebarNav.applyRolePermissions("GUEST");
            return;
        }

        String role = (user.getRole() != null) ? user.getRole().toUpperCase() : "MAHASISWA";
        lblRoleBadge.setText(user.getRoleDisplayName().toUpperCase());
        lblGlobalUserContext.setText("CampusRoom PRO 1.0 • Peran Aktif: " + user.getRoleDisplayName() + " (" + user.getFullName() + ")");

        sidebarNav.applyRolePermissions(role);

        if (role.contains("MAHASISWA")) {
            lblRoleBadge.setBackground(new Color(224, 242, 254)); // Sky 100
            lblRoleBadge.setForeground(new Color(2, 132, 199));   // Sky 600
            if (sidebarNav.getSelectedIndex() > 2) {
                switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);
            }
        } else if (role.contains("SEKDEK") || role.contains("SEKRETARIS")) {
            lblRoleBadge.setBackground(new Color(243, 232, 255)); // Purple 100
            lblRoleBadge.setForeground(new Color(126, 34, 206));  // Purple 700
            if (sidebarNav.getSelectedIndex() > 4) {
                switchToView(SidebarNavigationPanel.NAV_SEC_VALIDATION);
            }
        } else {
            // ADMIN MASTER
            lblRoleBadge.setBackground(new Color(220, 252, 231)); // Emerald 100
            lblRoleBadge.setForeground(new Color(22, 101, 52));   // Emerald 800
        }
    }

    @Override
    public void onRoomSelected(Room room) {
        this.selectedRoom = room;
        if (room == null) return;

        lblSidebarRoomTitle.setText(room.getCode() + " - " + room.getName());
        boolean isAvail = room.isAvailable();
        if (isAvail) {
            lblSidebarStatusBanner.setText("● STATUS: TERSEDIA");
            lblSidebarStatusBanner.setBackground(new Color(236, 253, 245));
            lblSidebarStatusBanner.setForeground(new Color(5, 150, 105));
            btnSidebarBook.setEnabled(true);
            btnSidebarBook.setText("Ajukan Booking Ruangan ➔");
            btnSidebarBook.setBackground(ModernUIHelper.COLOR_SUCCESS);
        } else {
            lblSidebarStatusBanner.setText("■ STATUS: TERPAKAI");
            lblSidebarStatusBanner.setBackground(new Color(254, 242, 242));
            lblSidebarStatusBanner.setForeground(new Color(220, 38, 38));
            btnSidebarBook.setEnabled(false);
            btnSidebarBook.setText("Ruangan Terpakai");
            btnSidebarBook.setBackground(new Color(148, 163, 184));
        }

        lblSidebarCapacity.setText("<html><b style='color:#64748b; font-size:9px;'>KAPASITAS</b><br/><span style='color:#0f172a; font-weight:bold;'>" + room.getCapacity() + " Kursi</span> <span style='color:#64748b;'>(" + room.getRoomType() + ")</span></html>");
        lblSidebarFloor.setText("<html><b style='color:#64748b; font-size:9px;'>LOKASI LANTAI</b><br/><span style='color:#0f172a; font-weight:bold;'>Lantai " + room.getFloor() + "</span></html>");
        lblSidebarFacilities.setText("<html><b style='color:#64748b; font-size:9px;'>FASILITAS</b><br/><span style='color:#334155; font-size:11px;'>" + room.getFacilities() + "</span></html>");
        lblSidebarOccupant.setText("<html><b style='color:#64748b; font-size:9px;'>STATUS PENGGUNAAN</b><br/><span style='color:#334155; font-size:11px;'>" + room.getCurrentOccupantInfo() + "</span></html>");
    }

    @Override
    public void onRoomDoubleClicked(Room room) {
        openDetailDialog(room);
    }

    @Override
    public void onRoomMoved(Room room, int newX, int newY) {
        // Hanya di LayoutEditorPanel
    }

    private void openBookingDialog(Room room) {
        LocalDate date;
        try {
            date = LocalDate.parse(txtDate.getText().trim());
        } catch (Exception e) {
            date = LocalDate.now();
        }

        String slotStr = (String) cbSlot.getSelectedItem();
        String startTime = "08:00";
        String endTime = "10:30";
        if (slotStr != null && slotStr.contains("-")) {
            String clean = slotStr.substring(0, slotStr.indexOf("(")).trim();
            String[] parts = clean.split("-");
            startTime = parts[0].trim();
            endTime = parts[1].trim();
        }

        BookingDialog dialog = new BookingDialog(this, room, date, startTime, endTime, () -> {
            refreshAvailability();
            switchToView(SidebarNavigationPanel.NAV_BOOKING);
        });
        dialog.setVisible(true);
    }

    private void openDetailDialog(Room room) {
        LocalDate date;
        try {
            date = LocalDate.parse(txtDate.getText().trim());
        } catch (Exception e) {
            date = LocalDate.now();
        }

        String slotStr = (String) cbSlot.getSelectedItem();
        String startTime = "08:00";
        String endTime = "10:30";
        if (slotStr != null && slotStr.contains("-")) {
            String clean = slotStr.substring(0, slotStr.indexOf("(")).trim();
            String[] parts = clean.split("-");
            startTime = parts[0].trim();
            endTime = parts[1].trim();
        }

        RoomDetailDialog dialog = new RoomDetailDialog(this, room, date, startTime, endTime, () -> openBookingDialog(room));
        dialog.setVisible(true);
    }
}
