package com.campusroom.ui;

import com.campusroom.model.Booking;
import com.campusroom.model.Building;
import com.campusroom.model.Campus;
import com.campusroom.model.RegularSchedule;
import com.campusroom.model.Room;
import com.campusroom.model.User;
import com.campusroom.service.AuthService;
import com.campusroom.service.AvailabilityService;
import com.campusroom.service.BookingService;
import com.campusroom.service.MasterDataService;
import com.campusroom.service.ScheduleService;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
    private final MasterDataService masterDataService = new MasterDataService();
    private final BookingService bookingService = new BookingService();
    private final AvailabilityService availabilityService = new AvailabilityService();
    private final ScheduleService scheduleService = new ScheduleService();
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

    // Zoom Controls
    private JLabel lblZoomLevel;
    private JButton btnZoomIn;
    private JButton btnZoomOut;
    private JButton btnResetZoom;

    // POV Rotation Controls
    private JLabel lblRotationAngle;
    private JButton btnRotateCW;
    private JButton btnRotateCCW;
    private JButton btnResetRotation;

    // Quick Search on Floor Plan
    private JTextField txtFloorSearch;

    // Card Layout Views
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private InteractiveFloorPlanPanel floorCanvas;
    private RoomCatalogPanel catalogPanel;
    private StudentBookingPanel studentBookingPanel;
    private SecretaryPanel secretaryPanel;
    private AdminPanel adminPanel;
    private LayoutEditorPanel layoutEditorPanel;

    // Detail Ruangan Inspector (Dock Kanan Sleek & Non-Intrusive)
    public static final int INSPECTOR_WIDTH = 315;
    public static final int INSPECTOR_COLLAPSED_WIDTH = 0;
    public static final int INSPECTOR_EXPANDED_WIDTH = 315;

    private JPanel roomDetailSidebar;
    private CardLayout inspectorBodyCardLayout;
    private JPanel inspectorBodyContainer;
    private JButton btnCloseInspector;
    private JButton btnToggleInspector;
    private boolean isInspectorVisible = false;

    private JLabel lblSidebarRoomTitle;
    private JLabel lblSidebarStatusBanner;
    private JLabel lblSidebarCapacity;
    private JLabel lblSidebarFloor;
    private JLabel lblSidebarFacilities;
    private JLabel lblSidebarOccupant;
    private JLabel lblSidebarAgendaTitle;
    private JComboBox<String> cbSidebarScheduleScope;
    private JPanel pnlScheduleListContainer;
    private JScrollPane scrollScheduleList;
    private int sidebarScheduleCardCount = 0;
    private JButton btnSidebarBook;

    // Floor Plan Stats and Legend (Scoped to Floor Plan)
    private JLabel lblFloorStats;

    // Global Bottom Status Bar
    private JLabel lblGlobalDbStatus;
    private JLabel lblGlobalUserContext;

    private List<Room> currentRooms;
    private Room selectedRoom;

    // Dirty State Flags untuk Persistensi State UI antar navigasi menu
    private boolean isFloorPlanDirty = false;
    private boolean isStudentBookingDirty = false;
    private boolean isSecretaryDirty = false;
    private boolean isAdminDirty = false;
    private boolean isEditorInitialized = false;

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
                        "Pilih ruangan yang tersedia (hijau) pada denah terlebih dahulu.",
                        "Pilih Ruangan", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        cardPanel.add(studentBookingPanel, "booking");

        // View 3: Validasi Pengajuan (Sekdek)
        secretaryPanel = new SecretaryPanel(() -> {
            isFloorPlanDirty = true;
            isStudentBookingDirty = true;
            isAdminDirty = true;
            refreshAvailability();
        });
        cardPanel.add(secretaryPanel.getValidationPanel(), "secValidation");

        // View 4: Jadwal Tetap Perkuliahan (Sekdek)
        cardPanel.add(secretaryPanel.getSchedulePanel(), "secSchedule");

        // View 5, 6, 7: Portal Admin Master
        adminPanel = new AdminPanel(() -> {
            loadCampuses();
            if (layoutEditorPanel != null) {
                layoutEditorPanel.loadCampuses();
            }
            isFloorPlanDirty = true;
            isStudentBookingDirty = true;
            isSecretaryDirty = true;
            refreshAvailability();
        });
        cardPanel.add(adminPanel.getApprovalPanel(), "adminApproval");
        cardPanel.add(adminPanel.getCampusBuildingPanel(), "adminCampus");
        cardPanel.add(adminPanel.getRoomManagementPanel(), "adminRooms");

        // View 8: Visual Layout Editor
        layoutEditorPanel = new LayoutEditorPanel(() -> {
            syncLocationFromEditor();
            isAdminDirty = true;
            refreshAvailability();
        });
        cardPanel.add(layoutEditorPanel, "layoutEditor");

        mainContentPanel.add(cardPanel, BorderLayout.CENTER);

        // Bottom Global Status Bar
        mainContentPanel.add(createBottomStatusBar(), BorderLayout.SOUTH);

        getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        updateRoleView(AuthService.getInstance().getCurrentUser());

        // Keyboard shortcut: Ctrl + B (atau Cmd + B) untuk buka / ciutkan sidebar navigasi
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

        // Keyboard shortcut: Ctrl + I (atau Cmd + I) untuk buka / ciutkan inspector ruangan (Left Bar)
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "toggleInspector"
        );
        getRootPane().getActionMap().put("toggleInspector", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleInspector();
            }
        });

        // Keyboard shortcut: Ctrl + R (atau Cmd + R) untuk putar denah searah jarum jam (+90°)
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "rotateFloorPlanCW"
        );
        getRootPane().getActionMap().put("rotateFloorPlanCW", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (floorCanvas != null) {
                    floorCanvas.rotateClockwise();
                }
            }
        });

        // Keyboard shortcut: Ctrl + Shift + R untuk reset orientasi denah ke 0°
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK),
                "resetFloorPlanRotation"
        );
        getRootPane().getActionMap().put("resetFloorPlanRotation", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (floorCanvas != null) {
                    floorCanvas.resetRotation();
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

        JButton btnContactAdmin = ModernUIHelper.createStyledButton("WA Admin", new Color(220, 252, 231), new Color(22, 101, 52), 11, true);
        btnContactAdmin.setToolTipText("Hubungi Admin Sarpras langsung via WhatsApp (" + ModernUIHelper.ADMIN_PHONE + ")");
        btnContactAdmin.addActionListener(e -> ModernUIHelper.showContactAdminDialog(this));

        JButton btnQuickSwitch = ModernUIHelper.createStyledButton("Ganti Akun", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnQuickSwitch.addActionListener(e -> showRoleSwitchMenu());

        rightPanel.add(btnContactAdmin);
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
     * Tampilan Denah Visual:
     * - Ribbon Bar Minimalis di bagian atas (North)
     * - Canvas Denah Luas di bagian tengah (Center)
     * - Detail Ruangan Inspector di dock kanan (East), tertutup rapi secara default dan
     *   terbuka mulus saat ruangan diklik atau tombol Detail ditekan.
     */
    private JPanel createFloorPlanTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        panel.setBackground(ModernUIHelper.BG_PAGE);

        // 1. North: Modern Single-Row Filter Ribbon
        panel.add(createFloorPlanFilterCard(), BorderLayout.NORTH);

        // 2. Center: Canvas Denah
        floorCanvas = new InteractiveFloorPlanPanel();
        floorCanvas.setFloorPlanListener(this);
        floorCanvas.addZoomChangeListener(zoom -> {
            if (lblZoomLevel != null) {
                lblZoomLevel.setText((int) Math.round(zoom * 100) + "%");
            }
        });
        floorCanvas.addRotationChangeListener(degrees -> {
            if (lblRotationAngle != null) {
                lblRotationAngle.setText(degrees + "°");
            }
        });
        JScrollPane canvasScroll = new JScrollPane(floorCanvas);
        canvasScroll.setBorder(BorderFactory.createLineBorder(ModernUIHelper.BORDER_LIGHT));
        canvasScroll.getVerticalScrollBar().setUnitIncrement(16);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(16);
        panel.add(canvasScroll, BorderLayout.CENTER);

        // 3. East: Detail Ruangan Inspector (Dock Kanan Rapi & Non-Intrusive)
        panel.add(createRoomDetailSidebar(), BorderLayout.EAST);

        return panel;
    }

    private JPanel createFloorPlanFilterCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(ModernUIHelper.BORDER_LIGHT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        // --- ROW 1: Filter Capsules & Main Actions ---
        JPanel row1 = new JPanel(new BorderLayout(8, 0));
        row1.setOpaque(false);

        JPanel row1Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row1Left.setOpaque(false);

        // Kapsul 1: Lokasi Kampus -> Gedung -> Lantai
        JPanel locCapsule = ModernUIHelper.createFilterCapsule(new FlowLayout(FlowLayout.LEFT, 4, 1));

        JLabel lblLocTag = new JLabel("Lokasi:");
        lblLocTag.setFont(ModernUIHelper.fontBold(10));
        lblLocTag.setForeground(ModernUIHelper.TEXT_SECONDARY);
        locCapsule.add(lblLocTag);

        cbCampus = new JComboBox<>();
        cbCampus.setPreferredSize(new Dimension(92, 26));
        ModernUIHelper.styleComboBox(cbCampus);
        cbCampus.addActionListener(e -> onCampusSelected());
        locCapsule.add(cbCampus);

        JLabel sep1 = new JLabel("›");
        sep1.setFont(ModernUIHelper.fontBold(11));
        sep1.setForeground(ModernUIHelper.TEXT_MUTED);
        locCapsule.add(sep1);

        cbBuilding = new JComboBox<>();
        cbBuilding.setPreferredSize(new Dimension(88, 26));
        ModernUIHelper.styleComboBox(cbBuilding);
        cbBuilding.addActionListener(e -> onBuildingSelected());
        locCapsule.add(cbBuilding);

        JLabel sep2 = new JLabel("›");
        sep2.setFont(ModernUIHelper.fontBold(11));
        sep2.setForeground(ModernUIHelper.TEXT_MUTED);
        locCapsule.add(sep2);

        cbFloor = new JComboBox<>();
        cbFloor.setPreferredSize(new Dimension(65, 26));
        ModernUIHelper.styleComboBox(cbFloor);
        cbFloor.setFont(ModernUIHelper.fontBold(11));
        cbFloor.addActionListener(e -> {
            if (!isUpdatingFloorCombo) refreshAvailability();
        });
        locCapsule.add(cbFloor);

        row1Left.add(locCapsule);

        // Kapsul 2: Tanggal & Sesi Waktu
        JPanel scheduleCapsule = ModernUIHelper.createFilterCapsule(new FlowLayout(FlowLayout.LEFT, 4, 1));

        JLabel lblDateTag = new JLabel("Tgl:");
        lblDateTag.setFont(ModernUIHelper.fontBold(10));
        lblDateTag.setForeground(ModernUIHelper.TEXT_SECONDARY);
        scheduleCapsule.add(lblDateTag);

        txtDate = new JTextField(LocalDate.now().toString(), 8);
        ModernUIHelper.styleTextField(txtDate);
        txtDate.setPreferredSize(new Dimension(70, 26));
        scheduleCapsule.add(txtDate);

        JSeparator vSep = new JSeparator(SwingConstants.VERTICAL);
        vSep.setPreferredSize(new Dimension(1, 16));
        vSep.setForeground(ModernUIHelper.BORDER_LIGHT);
        scheduleCapsule.add(vSep);

        JLabel lblSlotTag = new JLabel("Sesi:");
        lblSlotTag.setFont(ModernUIHelper.fontBold(10));
        lblSlotTag.setForeground(ModernUIHelper.TEXT_SECONDARY);
        scheduleCapsule.add(lblSlotTag);

        String[] slots = {
                "08:00 - 10:30 (Sesi Pagi 1)",
                "10:30 - 13:00 (Sesi Siang 1)",
                "13:00 - 15:30 (Sesi Siang 2)",
                "15:30 - 18:00 (Sesi Sore)",
                "18:00 - 20:30 (Sesi Malam)"
        };
        cbSlot = new JComboBox<>(slots);
        cbSlot.setPreferredSize(new Dimension(100, 26));
        ModernUIHelper.styleComboBox(cbSlot);
        cbSlot.addActionListener(e -> refreshAvailability());
        scheduleCapsule.add(cbSlot);

        row1Left.add(scheduleCapsule);

        row1.add(row1Left, BorderLayout.CENTER);

        // Aksi Kanan: Tombol Cek Jadwal & Toggle Detail Ruangan
        JPanel row1Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        row1Right.setOpaque(false);

        btnRefreshStatus = ModernUIHelper.createStyledButton("Cek Jadwal", ModernUIHelper.COLOR_PRIMARY, Color.WHITE, 11, true);
        btnRefreshStatus.setPreferredSize(new Dimension(92, 28));
        btnRefreshStatus.setBorder(new EmptyBorder(4, 8, 4, 8));
        btnRefreshStatus.addActionListener(e -> refreshAvailability());
        row1Right.add(btnRefreshStatus);

        btnToggleInspector = ModernUIHelper.createStyledButton("Detail Ruangan", new Color(241, 245, 249), ModernUIHelper.TEXT_DARK, 11, false);
        btnToggleInspector.setPreferredSize(new Dimension(102, 28));
        btnToggleInspector.setBorder(new EmptyBorder(4, 8, 4, 8));
        btnToggleInspector.setToolTipText("Buka / Tutup Inspector Detail Ruangan (Ctrl+I)");
        btnToggleInspector.addActionListener(e -> toggleInspector());
        row1Right.add(btnToggleInspector);

        row1.add(row1Right, BorderLayout.EAST);
        card.add(row1, BorderLayout.NORTH);

        // --- ROW 2: Canvas Zoom Toolbar & Live Floor Statistics ---
        JPanel row2 = new JPanel(new BorderLayout(8, 0));
        row2.setOpaque(false);

        // Kiri: Zoom Controller Group
        JPanel zoomGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        zoomGroup.setOpaque(false);

        JLabel lblZoomTitle = new JLabel("Zoom:");
        lblZoomTitle.setFont(ModernUIHelper.fontBold(10));
        lblZoomTitle.setForeground(ModernUIHelper.TEXT_SECONDARY);
        zoomGroup.add(lblZoomTitle);

        JPanel zoomPill = ModernUIHelper.createFilterCapsule(new FlowLayout(FlowLayout.CENTER, 2, 1));

        btnZoomOut = ModernUIHelper.createCompactButton("−", Color.WHITE, ModernUIHelper.TEXT_DARK, 13, true);
        btnZoomOut.setPreferredSize(new Dimension(28, 24));
        btnZoomOut.setToolTipText("Perkecil Denah (Ctrl + Scroll Down)");
        btnZoomOut.addActionListener(e -> {
            if (floorCanvas != null) floorCanvas.zoomOut();
        });
        zoomPill.add(btnZoomOut);

        lblZoomLevel = new JLabel("100%", SwingConstants.CENTER);
        lblZoomLevel.setFont(ModernUIHelper.fontBold(10));
        lblZoomLevel.setForeground(ModernUIHelper.COLOR_PRIMARY);
        lblZoomLevel.setPreferredSize(new Dimension(46, 24));
        lblZoomLevel.setToolTipText("Klik untuk reset ke 100%");
        lblZoomLevel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblZoomLevel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (floorCanvas != null) floorCanvas.resetZoom();
            }
        });
        zoomPill.add(lblZoomLevel);

        btnZoomIn = ModernUIHelper.createCompactButton("+", Color.WHITE, ModernUIHelper.TEXT_DARK, 13, true);
        btnZoomIn.setPreferredSize(new Dimension(28, 24));
        btnZoomIn.setToolTipText("Perbesar Denah (Ctrl + Scroll Up)");
        btnZoomIn.addActionListener(e -> {
            if (floorCanvas != null) floorCanvas.zoomIn();
        });
        zoomPill.add(btnZoomIn);

        btnResetZoom = ModernUIHelper.createCompactButton("Reset", Color.WHITE, ModernUIHelper.TEXT_SECONDARY, 10, false);
        btnResetZoom.setPreferredSize(new Dimension(48, 24));
        btnResetZoom.setToolTipText("Kembalikan ukuran denah ke normal (100%)");
        btnResetZoom.addActionListener(e -> {
            if (floorCanvas != null) floorCanvas.resetZoom();
        });
        zoomPill.add(btnResetZoom);

        zoomGroup.add(zoomPill);

        // Separator & POV Rotation Capsule
        JSeparator sepZoomPov = new JSeparator(SwingConstants.VERTICAL);
        sepZoomPov.setPreferredSize(new Dimension(1, 18));
        sepZoomPov.setForeground(ModernUIHelper.BORDER_LIGHT);
        zoomGroup.add(sepZoomPov);

        JLabel lblPovTitle = new JLabel("POV:");
        lblPovTitle.setFont(ModernUIHelper.fontBold(10));
        lblPovTitle.setForeground(ModernUIHelper.TEXT_SECONDARY);
        zoomGroup.add(lblPovTitle);

        JPanel povPill = ModernUIHelper.createFilterCapsule(new FlowLayout(FlowLayout.CENTER, 2, 1));

        btnRotateCCW = ModernUIHelper.createCompactButton("↺", Color.WHITE, ModernUIHelper.TEXT_DARK, 12, true);
        btnRotateCCW.setPreferredSize(new Dimension(28, 24));
        btnRotateCCW.setToolTipText("Putar Denah 90° Berlawanan Jarum Jam (Ctrl + Shift + R)");
        btnRotateCCW.addActionListener(e -> {
            if (floorCanvas != null) floorCanvas.rotateCounterClockwise();
        });
        povPill.add(btnRotateCCW);

        lblRotationAngle = new JLabel("0°", SwingConstants.CENTER);
        lblRotationAngle.setFont(ModernUIHelper.fontBold(10));
        lblRotationAngle.setForeground(ModernUIHelper.COLOR_PRIMARY);
        lblRotationAngle.setPreferredSize(new Dimension(38, 24));
        lblRotationAngle.setToolTipText("Sudut Pandang POV Aktif (Klik untuk Reset ke 0°)");
        lblRotationAngle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblRotationAngle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (floorCanvas != null) floorCanvas.resetRotation();
            }
        });
        povPill.add(lblRotationAngle);

        btnRotateCW = ModernUIHelper.createCompactButton("↻", Color.WHITE, ModernUIHelper.TEXT_DARK, 12, true);
        btnRotateCW.setPreferredSize(new Dimension(28, 24));
        btnRotateCW.setToolTipText("Putar Denah 90° Searah Jarum Jam (Ctrl + R)");
        btnRotateCW.addActionListener(e -> {
            if (floorCanvas != null) floorCanvas.rotateClockwise();
        });
        povPill.add(btnRotateCW);

        btnResetRotation = ModernUIHelper.createCompactButton("0°", Color.WHITE, ModernUIHelper.TEXT_SECONDARY, 10, false);
        btnResetRotation.setPreferredSize(new Dimension(34, 24));
        btnResetRotation.setToolTipText("Kembalikan Orientasi POV ke Default (0°)");
        btnResetRotation.addActionListener(e -> {
            if (floorCanvas != null) floorCanvas.resetRotation();
        });
        povPill.add(btnResetRotation);

        zoomGroup.add(povPill);

        // Separator & Quick Room Search Capsule
        JSeparator sepPovSearch = new JSeparator(SwingConstants.VERTICAL);
        sepPovSearch.setPreferredSize(new Dimension(1, 18));
        sepPovSearch.setForeground(ModernUIHelper.BORDER_LIGHT);
        zoomGroup.add(sepPovSearch);

        JPanel searchCapsule = ModernUIHelper.createFilterCapsule(new FlowLayout(FlowLayout.LEFT, 4, 1));
        JLabel lblSearchTag = new JLabel("Cari:");
        lblSearchTag.setFont(ModernUIHelper.fontBold(10));
        lblSearchTag.setForeground(ModernUIHelper.TEXT_SECONDARY);
        searchCapsule.add(lblSearchTag);

        txtFloorSearch = new JTextField(12);
        txtFloorSearch.setPreferredSize(new Dimension(125, 24));
        txtFloorSearch.putClientProperty("JTextField.placeholderText", "Kode / nama ruang...");
        txtFloorSearch.setToolTipText("Ketik kode atau nama ruangan untuk sorot langsung (Enter: cari semua lantai)");
        ModernUIHelper.styleTextField(txtFloorSearch);
        txtFloorSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { performRoomSearch(txtFloorSearch.getText().trim(), false); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { performRoomSearch(txtFloorSearch.getText().trim(), false); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { performRoomSearch(txtFloorSearch.getText().trim(), false); }
        });
        txtFloorSearch.addActionListener(e -> performRoomSearch(txtFloorSearch.getText().trim(), true));
        searchCapsule.add(txtFloorSearch);

        JButton btnClearSearch = ModernUIHelper.createCompactButton("✕", Color.WHITE, ModernUIHelper.TEXT_SECONDARY, 10, false);
        btnClearSearch.setPreferredSize(new Dimension(22, 22));
        btnClearSearch.setToolTipText("Bersihkan pencarian ruangan");
        btnClearSearch.addActionListener(e -> {
            txtFloorSearch.setText("");
            if (floorCanvas != null) floorCanvas.setSearchHighlightRoom(null);
        });
        searchCapsule.add(btnClearSearch);

        zoomGroup.add(searchCapsule);

        row2.add(zoomGroup, BorderLayout.WEST);

        // Kanan: Legend & Live Floor Status
        JPanel row2Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        row2Right.setOpaque(false);

        JLabel leg1 = createPillBadge("Tersedia", new Color(16, 185, 129), new Color(220, 252, 231), new Color(22, 101, 52));
        JLabel leg2 = createPillBadge("Terpakai", new Color(239, 68, 68), new Color(254, 226, 226), new Color(153, 27, 27));
        lblFloorStats = createPillBadge("Total: 8 | Kosong: 8", null, new Color(241, 245, 249), ModernUIHelper.COLOR_PRIMARY);

        row2Right.add(leg1);
        row2Right.add(leg2);
        row2Right.add(lblFloorStats);

        row2.add(row2Right, BorderLayout.EAST);
        card.add(row2, BorderLayout.SOUTH);

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
        lbl.setFont(ModernUIHelper.fontBold(10));
        lbl.setOpaque(false);
        lbl.setBackground(bg);
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(4, dotColor != null ? 18 : 10, 4, 10));
        return lbl;
    }

    private JPanel createRoomDetailSidebar() {
        roomDetailSidebar = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(ModernUIHelper.BORDER_LIGHT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        roomDetailSidebar.setOpaque(false);
        roomDetailSidebar.setPreferredSize(new Dimension(isInspectorVisible ? INSPECTOR_WIDTH : 0, 500));
        roomDetailSidebar.setVisible(isInspectorVisible);
        roomDetailSidebar.setBorder(new EmptyBorder(14, 16, 14, 16));

        // 1. Header: Badge + Close (✕) Button
        JPanel topHeader = new JPanel(new BorderLayout(8, 0));
        topHeader.setOpaque(false);

        JLabel lblBadge = new JLabel("INSPECTOR RUANGAN");
        lblBadge.setFont(new Font("SansSerif", Font.BOLD, 9));
        lblBadge.setForeground(new Color(79, 70, 229)); // Indigo 600
        topHeader.add(lblBadge, BorderLayout.WEST);

        btnCloseInspector = new JButton("✕") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCloseInspector.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCloseInspector.setForeground(new Color(100, 116, 139));
        btnCloseInspector.setPreferredSize(new Dimension(24, 24));
        btnCloseInspector.setFocusPainted(false);
        btnCloseInspector.setBorderPainted(false);
        btnCloseInspector.setContentAreaFilled(false);
        btnCloseInspector.setOpaque(false);
        btnCloseInspector.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCloseInspector.setToolTipText("Tutup Inspector (Ctrl+I)");
        btnCloseInspector.addActionListener(e -> setInspectorVisible(false));
        topHeader.add(btnCloseInspector, BorderLayout.EAST);

        // Room Title and Status Banner
        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 6));
        titleBox.setOpaque(false);
        titleBox.setBorder(new EmptyBorder(8, 0, 8, 0));

        lblSidebarRoomTitle = new JLabel("Pilih Ruangan di Denah", JLabel.LEFT);
        lblSidebarRoomTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSidebarRoomTitle.setForeground(ModernUIHelper.TEXT_DARK);

        lblSidebarStatusBanner = new JLabel("Klik salah satu ruangan di denah", JLabel.CENTER) {
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
        lblSidebarStatusBanner.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblSidebarStatusBanner.setOpaque(false);
        lblSidebarStatusBanner.setBackground(new Color(241, 245, 249));
        lblSidebarStatusBanner.setForeground(new Color(100, 116, 139));
        lblSidebarStatusBanner.setBorder(new EmptyBorder(6, 10, 6, 10));

        titleBox.add(lblSidebarRoomTitle);
        titleBox.add(lblSidebarStatusBanner);

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.add(topHeader, BorderLayout.NORTH);
        headerWrapper.add(titleBox, BorderLayout.CENTER);
        roomDetailSidebar.add(headerWrapper, BorderLayout.NORTH);

        // 2. Center: Container dengan CardLayout ("spec" vs "empty")
        inspectorBodyCardLayout = new CardLayout();
        inspectorBodyContainer = new JPanel(inspectorBodyCardLayout);
        inspectorBodyContainer.setOpaque(false);

        // Card 1: Empty State
        inspectorBodyContainer.add(createInspectorEmptyStatePanel(), "empty");

        // Card 2: Specification Card (Compact Summary) + Scrollable Schedule Feed
        JPanel cardSpec = new JPanel(new GridLayout(4, 1, 0, 5));
        cardSpec.setBackground(new Color(248, 250, 252));
        cardSpec.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));

        lblSidebarCapacity = new JLabel("<html><b style='color:#64748b; font-size:9px;'>👥 KAPASITAS & TIPE</b><br/><span style='color:#334155;'>-</span></html>");
        lblSidebarFloor = new JLabel("<html><b style='color:#64748b; font-size:9px;'>📍 LOKASI LANTAI</b><br/><span style='color:#334155;'>-</span></html>");
        lblSidebarFacilities = new JLabel("<html><b style='color:#64748b; font-size:9px;'>🛠️ FASILITAS RUANGAN</b><br/><span style='color:#334155;'>-</span></html>");
        lblSidebarOccupant = new JLabel("<html><b style='color:#64748b; font-size:9px;'>📅 PENGGUNAAN SAAT INI</b><br/><span style='color:#334155;'>-</span></html>");

        cardSpec.add(lblSidebarCapacity);
        cardSpec.add(lblSidebarFloor);
        cardSpec.add(lblSidebarFacilities);
        cardSpec.add(lblSidebarOccupant);

        // Scrollable Schedule Agenda Section
        JPanel agendaSection = new JPanel(new BorderLayout(0, 6));
        agendaSection.setOpaque(false);

        JPanel agendaHeader = new JPanel(new BorderLayout(6, 0));
        agendaHeader.setOpaque(false);
        agendaHeader.setBorder(new EmptyBorder(2, 1, 0, 1));

        lblSidebarAgendaTitle = new JLabel("📅 JADWAL RUANGAN");
        lblSidebarAgendaTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblSidebarAgendaTitle.setForeground(new Color(71, 85, 105));

        cbSidebarScheduleScope = new JComboBox<>(new String[]{"Tanggal Ini", "Semua Hari"});
        cbSidebarScheduleScope.setFont(new Font("SansSerif", Font.PLAIN, 10));
        cbSidebarScheduleScope.setBackground(Color.WHITE);
        cbSidebarScheduleScope.setPreferredSize(new Dimension(105, 24));
        cbSidebarScheduleScope.setToolTipText("Pilih cakupan jadwal yang ditampilkan pada sidebar");
        cbSidebarScheduleScope.addActionListener(e -> {
            if (selectedRoom != null) {
                updateSidebarScheduleFeed(selectedRoom);
            }
        });

        agendaHeader.add(lblSidebarAgendaTitle, BorderLayout.WEST);
        agendaHeader.add(cbSidebarScheduleScope, BorderLayout.EAST);

        pnlScheduleListContainer = new JPanel();
        pnlScheduleListContainer.setLayout(new BoxLayout(pnlScheduleListContainer, BoxLayout.Y_AXIS));
        pnlScheduleListContainer.setBackground(new Color(248, 250, 252));
        pnlScheduleListContainer.setBorder(new EmptyBorder(6, 6, 6, 6));

        scrollScheduleList = new JScrollPane(pnlScheduleListContainer);
        scrollScheduleList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollScheduleList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollScheduleList.getVerticalScrollBar().setUnitIncrement(14);
        scrollScheduleList.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        scrollScheduleList.getViewport().setBackground(new Color(248, 250, 252));

        agendaSection.add(agendaHeader, BorderLayout.NORTH);
        agendaSection.add(scrollScheduleList, BorderLayout.CENTER);

        JPanel specWrapper = new JPanel(new BorderLayout(0, 8));
        specWrapper.setOpaque(false);
        specWrapper.add(cardSpec, BorderLayout.NORTH);
        specWrapper.add(agendaSection, BorderLayout.CENTER);

        JLabel lblHint = new JLabel("<html><i>* Scroll ke bawah untuk seluruh jadwal, klik 2x denah untuk detail lengkap.</i></html>");
        lblHint.setFont(new Font("SansSerif", Font.ITALIC, 9));
        lblHint.setForeground(new Color(148, 163, 184));
        lblHint.setBorder(new EmptyBorder(2, 2, 0, 2));
        specWrapper.add(lblHint, BorderLayout.SOUTH);

        inspectorBodyContainer.add(specWrapper, "spec");
        inspectorBodyCardLayout.show(inspectorBodyContainer, (selectedRoom == null) ? "empty" : "spec");
        roomDetailSidebar.add(inspectorBodyContainer, BorderLayout.CENTER);

        // 3. Bottom Actions
        JPanel bottom = new JPanel(new GridLayout(2, 1, 6, 6));
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

    private JPanel createInspectorEmptyStatePanel() {
        JPanel emptyPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        emptyPanel.setOpaque(false);
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setBorder(new EmptyBorder(24, 14, 24, 14));

        JLabel lblIcon = new JLabel("🗺️", JLabel.CENTER);
        lblIcon.setFont(new Font("SansSerif", Font.PLAIN, 28));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblEmptyTitle = new JLabel("Pilih Ruangan di Denah", JLabel.CENTER);
        lblEmptyTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblEmptyTitle.setForeground(new Color(51, 65, 85));
        lblEmptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblEmptyDesc = new JLabel("<html><center style='color:#64748b; font-size:10px; line-height:140%;'>" +
                "Klik salah satu kotak ruangan di denah untuk melihat fasilitas, kapasitas, status ketersediaan, dan jadwal sesi perkuliahan." +
                "</center></html>", JLabel.CENTER);
        lblEmptyDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblShortcutTip = new JLabel("Pintasan: Tekan Ctrl+I untuk buka / tutup", JLabel.CENTER);
        lblShortcutTip.setFont(new Font("SansSerif", Font.ITALIC, 9));
        lblShortcutTip.setForeground(new Color(148, 163, 184));
        lblShortcutTip.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyPanel.add(Box.createVerticalGlue());
        emptyPanel.add(lblIcon);
        emptyPanel.add(Box.createVerticalStrut(10));
        emptyPanel.add(lblEmptyTitle);
        emptyPanel.add(Box.createVerticalStrut(6));
        emptyPanel.add(lblEmptyDesc);
        emptyPanel.add(Box.createVerticalStrut(14));
        emptyPanel.add(lblShortcutTip);
        emptyPanel.add(Box.createVerticalGlue());

        return emptyPanel;
    }

    public void setInspectorVisible(boolean visible) {
        this.isInspectorVisible = visible;
        if (roomDetailSidebar != null) {
            roomDetailSidebar.setVisible(visible);
            roomDetailSidebar.setPreferredSize(new Dimension(visible ? INSPECTOR_WIDTH : 0, 500));
            roomDetailSidebar.revalidate();
            roomDetailSidebar.repaint();
            if (roomDetailSidebar.getParent() != null) {
                roomDetailSidebar.getParent().revalidate();
                roomDetailSidebar.getParent().repaint();
            }
        }
        if (btnToggleInspector != null) {
            if (visible) {
                btnToggleInspector.setText("✕ Tutup Detail");
                btnToggleInspector.setBackground(new Color(238, 242, 255));
                btnToggleInspector.setForeground(new Color(79, 70, 229));
            } else {
                btnToggleInspector.setText("ℹ️ Detail Ruangan");
                btnToggleInspector.setBackground(new Color(241, 245, 249));
                btnToggleInspector.setForeground(ModernUIHelper.TEXT_DARK);
            }
        }
    }

    public void setInspectorCollapsed(boolean collapsed) {
        setInspectorVisible(!collapsed);
    }

    public boolean isInspectorCollapsed() {
        return !isInspectorVisible;
    }

    public void toggleInspector() {
        setInspectorVisible(!isInspectorVisible);
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
                if (isFloorPlanDirty) {
                    refreshAvailability();
                    isFloorPlanDirty = false;
                }
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
                if (isStudentBookingDirty && studentBookingPanel != null) {
                    studentBookingPanel.refreshData();
                    isStudentBookingDirty = false;
                }
                break;
            case SidebarNavigationPanel.NAV_SEC_VALIDATION:
                cardLayout.show(cardPanel, "secValidation");
                lblBreadcrumbView.setText("Portal Dekanat / Validasi Pengajuan");
                lblViewSubtitle.setText("Verifikasi jadwal perkuliahan pengganti mahasiswa terhadap master jadwal perkuliahan tetap");
                if (isSecretaryDirty && secretaryPanel != null) {
                    secretaryPanel.refreshData();
                    isSecretaryDirty = false;
                }
                break;
            case SidebarNavigationPanel.NAV_SEC_SCHEDULE:
                cardLayout.show(cardPanel, "secSchedule");
                lblBreadcrumbView.setText("Portal Dekanat / Master Jadwal Perkuliahan");
                lblViewSubtitle.setText("Kelola data jadwal tetap perkuliahan semester untuk mengunci ruangan kuliah secara rutin");
                if (isSecretaryDirty && secretaryPanel != null) {
                    secretaryPanel.refreshData();
                    isSecretaryDirty = false;
                }
                break;
            case SidebarNavigationPanel.NAV_ADMIN_APPROVAL:
                cardLayout.show(cardPanel, "adminApproval");
                lblBreadcrumbView.setText("Administrator / Approval Akhir");
                lblViewSubtitle.setText("Persetujuan final reservasi ruangan kuliah dan penetapan alokasi resmi");
                if (isAdminDirty && adminPanel != null) {
                    adminPanel.refreshData();
                    isAdminDirty = false;
                }
                break;
            case SidebarNavigationPanel.NAV_ADMIN_CAMPUS:
                cardLayout.show(cardPanel, "adminCampus");
                lblBreadcrumbView.setText("Administrator / Kelola Kampus & Gedung");
                lblViewSubtitle.setText("Konfigurasi seluruh lokasi kampus cabang dan gedung bertingkat");
                if (isAdminDirty && adminPanel != null) {
                    adminPanel.refreshData();
                    isAdminDirty = false;
                }
                break;
            case SidebarNavigationPanel.NAV_ADMIN_ROOMS:
                cardLayout.show(cardPanel, "adminRooms");
                lblBreadcrumbView.setText("Administrator / Master Ruangan");
                lblViewSubtitle.setText("Inventaris seluruh ruangan kuliah, kapasitas kursi, tipe ruangan, dan fasilitas");
                if (isAdminDirty && adminPanel != null) {
                    adminPanel.refreshData();
                    isAdminDirty = false;
                }
                break;
            case SidebarNavigationPanel.NAV_LAYOUT_EDITOR:
                cardLayout.show(cardPanel, "layoutEditor");
                lblBreadcrumbView.setText("Administrator / Desainer Denah Visual");
                lblViewSubtitle.setText("Editor tata letak denah interaktif (Toolbar Excalidraw & Inspector Modal)");
                if (!isEditorInitialized) {
                    syncLocationToEditor();
                    isEditorInitialized = true;
                }
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
        List<Campus> campuses = masterDataService.getAllCampuses();
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
        List<Building> buildings = masterDataService.getBuildingsByCampus(c.getId());
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

    public Campus getSelectedCampus() {
        return (Campus) cbCampus.getSelectedItem();
    }

    public Building getSelectedBuilding() {
        return (Building) cbBuilding.getSelectedItem();
    }

    public int getSelectedFloor() {
        return getSelectedFloorNumber();
    }

    public void setSelectedLocation(int campusId, int buildingId, int floorNum) {
        isUpdatingFloorCombo = true;
        for (int i = 0; i < cbCampus.getItemCount(); i++) {
            Campus c = cbCampus.getItemAt(i);
            if (c != null && c.getId() == campusId) {
                cbCampus.setSelectedIndex(i);
                break;
            }
        }

        Campus c = (Campus) cbCampus.getSelectedItem();
        if (c != null) {
            cbBuilding.removeAllItems();
            List<Building> buildings = masterDataService.getBuildingsByCampus(c.getId());
            for (Building b : buildings) {
                cbBuilding.addItem(b);
            }
        }

        for (int i = 0; i < cbBuilding.getItemCount(); i++) {
            Building b = cbBuilding.getItemAt(i);
            if (b != null && b.getId() == buildingId) {
                cbBuilding.setSelectedIndex(i);
                break;
            }
        }

        Building b = (Building) cbBuilding.getSelectedItem();
        if (b != null) {
            cbFloor.removeAllItems();
            int totalFloors = Math.max(1, b.getTotalFloors());
            for (int i = 1; i <= totalFloors; i++) {
                cbFloor.addItem("Lantai " + i);
            }
        }

        String floorStr = "Lantai " + floorNum;
        for (int i = 0; i < cbFloor.getItemCount(); i++) {
            if (floorStr.equals(cbFloor.getItemAt(i))) {
                cbFloor.setSelectedIndex(i);
                break;
            }
        }
        isUpdatingFloorCombo = false;
        refreshAvailability();
    }

    private void syncLocationFromEditor() {
        if (layoutEditorPanel != null && layoutEditorPanel.getSelectedCampus() != null && layoutEditorPanel.getSelectedBuilding() != null) {
            Campus ec = layoutEditorPanel.getSelectedCampus();
            Building eb = layoutEditorPanel.getSelectedBuilding();
            int ef = layoutEditorPanel.getSelectedFloor();
            setSelectedLocation(ec.getId(), eb.getId(), ef);
        }
    }

    private void syncLocationToEditor() {
        if (layoutEditorPanel != null) {
            Campus mc = (Campus) cbCampus.getSelectedItem();
            Building mb = (Building) cbBuilding.getSelectedItem();
            int mf = getSelectedFloorNumber();
            if (mc != null && mb != null) {
                layoutEditorPanel.setSelectedLocation(mc.getId(), mb.getId(), mf);
            } else {
                layoutEditorPanel.onFloorChanged();
            }
        }
    }

    public void refreshAvailability() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;

        int floorNum = getSelectedFloorNumber();

        // 1. Muat Blueprint Gambar Denah untuk Gedung & Lantai Terpilih via MasterDataService
        String imgPath = masterDataService.getFloorPlanImage(b.getId(), floorNum);
        floorCanvas.loadFloorImage(imgPath);

        // 2. Muat Ruangan khusus untuk Gedung & Lantai Terpilih via MasterDataService
        currentRooms = masterDataService.getRoomsByBuildingAndFloor(b.getId(), floorNum);

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
        List<Room> allBuildingRooms = masterDataService.getRoomsByBuilding(b.getId());
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

        // Update badge counter di sidebar interaktif via BookingService
        if (sidebarNav != null) {
            try {
                int pendingSekdek = bookingService.getPendingSecretaryCount();
                sidebarNav.setBadgeCount(SidebarNavigationPanel.NAV_SEC_VALIDATION, pendingSekdek);

                int pendingAdmin = bookingService.getPendingAdminCount();
                sidebarNav.setBadgeCount(SidebarNavigationPanel.NAV_ADMIN_APPROVAL, pendingAdmin);
            } catch (Exception ignored) {
            }
        }

        // Refresh data di panel lain
        if (studentBookingPanel != null) studentBookingPanel.refreshData();
        if (secretaryPanel != null) secretaryPanel.refreshData();
        if (adminPanel != null) adminPanel.refreshData();

        isFloorPlanDirty = false;
        isStudentBookingDirty = false;
        isSecretaryDirty = false;
        isAdminDirty = false;
    }

    public InteractiveFloorPlanPanel getFloorCanvas() {
        return floorCanvas;
    }

    public Room getSelectedRoom() {
        return selectedRoom;
    }

    private void resetSidebarInfo() {
        selectedRoom = null;
        if (inspectorBodyCardLayout != null && inspectorBodyContainer != null) {
            inspectorBodyCardLayout.show(inspectorBodyContainer, "empty");
        }
        if (lblSidebarRoomTitle != null) lblSidebarRoomTitle.setText("Pilih Ruangan di Denah");
        if (lblSidebarStatusBanner != null) {
            lblSidebarStatusBanner.setText("Klik salah satu kotak ruangan di denah");
            lblSidebarStatusBanner.setBackground(new Color(241, 245, 249));
            lblSidebarStatusBanner.setForeground(new Color(100, 116, 139));
        }
        if (lblSidebarCapacity != null) lblSidebarCapacity.setText("<html><b style='color:#64748b; font-size:9px;'>👥 KAPASITAS & TIPE</b><br/><span style='color:#94a3b8;'>-</span></html>");
        if (lblSidebarFloor != null) lblSidebarFloor.setText("<html><b style='color:#64748b; font-size:9px;'>📍 LOKASI LANTAI</b><br/><span style='color:#94a3b8;'>-</span></html>");
        if (lblSidebarFacilities != null) lblSidebarFacilities.setText("<html><b style='color:#64748b; font-size:9px;'>🛠️ FASILITAS RUANGAN</b><br/><span style='color:#94a3b8;'>-</span></html>");
        if (lblSidebarOccupant != null) lblSidebarOccupant.setText("<html><b style='color:#64748b; font-size:9px;'>📅 PENGGUNAAN SAAT INI</b><br/><span style='color:#94a3b8;'>-</span></html>");
        updateSidebarScheduleFeed(null);
        if (btnSidebarBook != null) {
            btnSidebarBook.setEnabled(false);
            btnSidebarBook.setText("Ajukan Booking Ruangan");
            btnSidebarBook.setBackground(ModernUIHelper.COLOR_SUCCESS);
        }
        if (roomDetailSidebar != null) {
            roomDetailSidebar.repaint();
        }
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
                "Yakin ingin keluar?",
                "Keluar",
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
        if (room == null) {
            resetSidebarInfo();
            return;
        }

        // Auto-expand inspector left bar saat ruangan dipilih agar pengguna langsung melihat informasi
        if (isInspectorCollapsed()) {
            setInspectorCollapsed(false);
        }
        if (inspectorBodyCardLayout != null && inspectorBodyContainer != null) {
            inspectorBodyCardLayout.show(inspectorBodyContainer, "spec");
        }

        lblSidebarRoomTitle.setText(room.getCode() + " • " + room.getName());
        boolean isAvail = room.isAvailable();
        if (isAvail) {
            lblSidebarStatusBanner.setText("● STATUS: TERSEDIA (Bebas Reservasi)");
            lblSidebarStatusBanner.setBackground(new Color(236, 253, 245));
            lblSidebarStatusBanner.setForeground(new Color(5, 150, 105));
            btnSidebarBook.setEnabled(true);
            btnSidebarBook.setText("Ajukan Booking Ruangan ➔");
            btnSidebarBook.setBackground(ModernUIHelper.COLOR_SUCCESS);
        } else {
            lblSidebarStatusBanner.setText("■ STATUS: TERPAKAI (Sedang Digunakan)");
            lblSidebarStatusBanner.setBackground(new Color(254, 242, 242));
            lblSidebarStatusBanner.setForeground(new Color(220, 38, 38));
            btnSidebarBook.setEnabled(false);
            btnSidebarBook.setText("Ruangan Terpakai");
            btnSidebarBook.setBackground(new Color(203, 213, 225));
        }

        lblSidebarCapacity.setText("<html><b style='color:#64748b; font-size:9px;'>👥 KAPASITAS & TIPE</b><br/><span style='color:#0f172a; font-weight:bold; font-size:11px;'>" + room.getCapacity() + " Kursi</span> &nbsp;<span style='color:#64748b;'>(" + room.getRoomType() + ")</span></html>");
        lblSidebarFloor.setText("<html><b style='color:#64748b; font-size:9px;'>📍 LOKASI LANTAI</b><br/><span style='color:#0f172a; font-weight:bold; font-size:11px;'>Lantai " + room.getFloor() + "</span></html>");
        lblSidebarFacilities.setText("<html><b style='color:#64748b; font-size:9px;'>🛠️ FASILITAS RUANGAN</b><br/><span style='color:#334155; font-size:11px;'>" + room.getFacilities() + "</span></html>");
        lblSidebarOccupant.setText("<html><b style='color:#64748b; font-size:9px;'>📅 PENGGUNAAN SAAT INI</b><br/><span style='color:#334155; font-size:11px;'>" + room.getCurrentOccupantInfo() + "</span></html>");
        updateSidebarScheduleFeed(room);
    }

    /**
     * Memperbarui daftar kartu jadwal yang dapat di-scroll di sidebar kanan sesuai ruangan & cakupan waktu aktif.
     */
    private void updateSidebarScheduleFeed(Room room) {
        sidebarScheduleCardCount = 0;
        if (pnlScheduleListContainer == null) {
            return;
        }
        pnlScheduleListContainer.removeAll();

        if (room == null) {
            if (lblSidebarAgendaTitle != null) {
                lblSidebarAgendaTitle.setText("📅 JADWAL RUANGAN");
            }
            pnlScheduleListContainer.revalidate();
            pnlScheduleListContainer.repaint();
            return;
        }

        LocalDate targetDate = LocalDate.now();
        if (txtDate != null) {
            try {
                String dateStr = txtDate.getText().trim();
                if (!dateStr.isEmpty()) {
                    targetDate = LocalDate.parse(dateStr);
                }
            } catch (DateTimeParseException ignored) {
                targetDate = LocalDate.now();
            }
        }

        String targetDayIndo = AvailabilityService.convertDayOfWeekToIndonesian(targetDate.getDayOfWeek());
        boolean showAllDays = cbSidebarScheduleScope != null && cbSidebarScheduleScope.getSelectedIndex() == 1;

        List<RegularSchedule> regularSchedules = showAllDays
                ? scheduleService.getSchedulesByRoomId(room.getId())
                : scheduleService.getSchedulesByRoomAndDay(room.getId(), targetDayIndo);

        List<Booking> allBookings = bookingService.getAllIncomingBookingsForRoom(room.getId());
        List<Booking> filteredBookings = new ArrayList<>();
        for (Booking b : allBookings) {
            if (showAllDays || (b.getBookingDate() != null && b.getBookingDate().equals(targetDate))) {
                filteredBookings.add(b);
            }
        }

        int totalItems = regularSchedules.size() + filteredBookings.size();
        if (lblSidebarAgendaTitle != null) {
            String scopeLabel = showAllDays ? "SEMUA JADWAL" : "JADWAL " + targetDayIndo.toUpperCase();
            lblSidebarAgendaTitle.setText("📅 " + scopeLabel + " (" + totalItems + ")");
        }

        if (totalItems == 0) {
            pnlScheduleListContainer.add(createEmptyScheduleTile(showAllDays, targetDayIndo, targetDate));
        } else {
            for (RegularSchedule schedule : regularSchedules) {
                if (sidebarScheduleCardCount > 0) {
                    pnlScheduleListContainer.add(Box.createVerticalStrut(6));
                }
                String timeText = (showAllDays ? schedule.getDayOfWeek() + " • " : "")
                        + schedule.getStartTime() + " - " + schedule.getEndTime();
                String subtitle = "Dosen: " + safeText(schedule.getLecturerName(), "-")
                        + " • Kelas " + safeText(schedule.getClassName(), "-");
                pnlScheduleListContainer.add(createScheduleCardItem(
                        "KULIAH TETAP",
                        new Color(59, 130, 246),
                        new Color(239, 246, 255),
                        new Color(29, 78, 216),
                        timeText,
                        safeText(schedule.getCourseName(), "-"),
                        subtitle
                ));
                sidebarScheduleCardCount++;
            }

            for (Booking booking : filteredBookings) {
                if (sidebarScheduleCardCount > 0) {
                    pnlScheduleListContainer.add(Box.createVerticalStrut(6));
                }
                boolean isFixed = booking.isFixed();
                Color accent = isFixed ? new Color(16, 185, 129) : new Color(245, 158, 11);
                Color badgeBg = isFixed ? new Color(236, 253, 245) : new Color(255, 251, 235);
                Color badgeFg = isFixed ? new Color(5, 150, 105) : new Color(180, 83, 9);
                String badgeText = isFixed ? "BOOKING FIKS" : "BOOKING PROSES";
                String datePrefix = (showAllDays && booking.getBookingDate() != null)
                        ? booking.getBookingDate() + " • "
                        : "";
                String timeText = datePrefix + booking.getStartTime() + " - " + booking.getEndTime();
                String subtitle = "PJ: " + safeText(booking.getStudentName(), "-")
                        + " (" + safeText(booking.getStatusBadgeText(), booking.getStatus()) + ")";
                pnlScheduleListContainer.add(createScheduleCardItem(
                        badgeText,
                        accent,
                        badgeBg,
                        badgeFg,
                        timeText,
                        safeText(booking.getCourseName(), safeText(booking.getPurpose(), "Booking Ruangan")),
                        subtitle
                ));
                sidebarScheduleCardCount++;
            }
        }

        pnlScheduleListContainer.revalidate();
        pnlScheduleListContainer.repaint();
        if (scrollScheduleList != null) {
            SwingUtilities.invokeLater(() -> scrollScheduleList.getViewport().setViewPosition(new Point(0, 0)));
        }
    }

    private JPanel createScheduleCardItem(
            String badgeText,
            Color accentColor,
            Color badgeBg,
            Color badgeFg,
            String timeText,
            String titleText,
            String subtitleText
    ) {
        JPanel card = new JPanel(new BorderLayout(0, 3));
        card.setBackground(Color.WHITE);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 4, 1, 1, accentColor),
                new EmptyBorder(6, 8, 6, 8)
        ));

        JPanel topRow = new JPanel(new BorderLayout(4, 0));
        topRow.setOpaque(false);

        JLabel lblTime = new JLabel(timeText);
        lblTime.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTime.setForeground(new Color(51, 65, 85));

        JLabel lblBadge = new JLabel(badgeText);
        lblBadge.setFont(new Font("SansSerif", Font.BOLD, 8));
        lblBadge.setOpaque(true);
        lblBadge.setBackground(badgeBg);
        lblBadge.setForeground(badgeFg);
        lblBadge.setBorder(new EmptyBorder(2, 5, 2, 5));

        topRow.add(lblTime, BorderLayout.WEST);
        topRow.add(lblBadge, BorderLayout.EAST);

        JLabel lblContent = new JLabel(
                "<html><div style='width:225px;'>"
                        + "<b style='color:#0f172a; font-size:10px;'>" + escapeHtml(titleText) + "</b><br/>"
                        + "<span style='color:#64748b; font-size:9px;'>" + escapeHtml(subtitleText) + "</span>"
                        + "</div></html>"
        );

        card.add(topRow, BorderLayout.NORTH);
        card.add(lblContent, BorderLayout.CENTER);
        Dimension pref = card.getPreferredSize();
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height + 4));
        return card;
    }

    private JPanel createEmptyScheduleTile(boolean showAllDays, String targetDayIndo, LocalDate targetDate) {
        JPanel emptyTile = new JPanel(new BorderLayout());
        emptyTile.setBackground(Color.WHITE);
        emptyTile.setAlignmentX(Component.LEFT_ALIGNMENT);
        emptyTile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(14, 10, 14, 10)
        ));

        String text = showAllDays
                ? "Tidak ada jadwal tetap maupun pengajuan booking aktif untuk ruangan ini."
                : "Tidak ada jadwal kelas atau booking pada " + targetDayIndo + " (" + targetDate + ").";
        JLabel lblEmpty = new JLabel(
                "<html><div style='width:225px; text-align:center;'>"
                        + "<span style='color:#059669; font-weight:bold; font-size:10px;'>✔ Jadwal Kosong</span><br/>"
                        + "<span style='color:#64748b; font-size:9px;'>" + escapeHtml(text) + "</span>"
                        + "</div></html>",
                SwingConstants.CENTER
        );
        emptyTile.add(lblEmpty, BorderLayout.CENTER);
        Dimension pref = emptyTile.getPreferredSize();
        emptyTile.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height + 6));
        return emptyTile;
    }

    private String safeText(String value, String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public JScrollPane getSidebarScheduleScrollPane() {
        return scrollScheduleList;
    }

    public int getSidebarScheduleCardCount() {
        return sidebarScheduleCardCount;
    }

    public JComboBox<String> getSidebarScheduleScopeCombo() {
        return cbSidebarScheduleScope;
    }

    public String getSidebarAgendaTitleText() {
        return lblSidebarAgendaTitle != null ? lblSidebarAgendaTitle.getText() : "";
    }

    @Override
    public void onRoomDoubleClicked(Room room) {
        openDetailDialog(room);
    }

    @Override
    public void onRoomMoved(Room room, int newX, int newY) {
        // Hanya di LayoutEditorPanel
    }

    private void performRoomSearch(String query, boolean searchAcrossBuilding) {
        if (floorCanvas == null) return;
        if (query == null || query.isEmpty()) {
            floorCanvas.setSearchHighlightRoom(null);
            return;
        }

        String lower = query.toLowerCase();
        Room matched = findBestRoomMatch(currentRooms, lower);

        if (matched == null && searchAcrossBuilding) {
            Building b = (Building) cbBuilding.getSelectedItem();
            Campus c = (Campus) cbCampus.getSelectedItem();
            if (b != null && c != null) {
                List<Room> buildingRooms = masterDataService.getRoomsByBuilding(b.getId());
                Room crossFloorMatch = findBestRoomMatch(buildingRooms, lower);
                if (crossFloorMatch != null) {
                    setSelectedLocation(c.getId(), b.getId(), crossFloorMatch.getFloor());
                    matched = findBestRoomMatch(currentRooms, lower);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Ruangan dengan kata kunci \"" + query + "\" tidak ditemukan di " + b.getName() + ".",
                            "Pencarian Ruangan", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }

        if (matched != null) {
            floorCanvas.setSearchHighlightRoom(matched);
            floorCanvas.setSelectedRoom(matched);
            onRoomSelected(matched);
            centerCanvasOnRoom(matched);
        } else {
            floorCanvas.setSearchHighlightRoom(null);
        }
    }

    private Room findBestRoomMatch(List<Room> roomList, String lowerQuery) {
        if (roomList == null || roomList.isEmpty()) return null;
        // Prioritaskan kecocokan kode persis atau awalan kode
        for (Room r : roomList) {
            if (r.getCode() != null && r.getCode().toLowerCase().equals(lowerQuery)) {
                return r;
            }
        }
        for (Room r : roomList) {
            if (r.getCode() != null && r.getCode().toLowerCase().contains(lowerQuery)) {
                return r;
            }
        }
        for (Room r : roomList) {
            if (r.getName() != null && r.getName().toLowerCase().contains(lowerQuery)) {
                return r;
            }
        }
        return null;
    }

    private void centerCanvasOnRoom(Room room) {
        if (floorCanvas == null || room == null) return;
        Rectangle bounds = floorCanvas.getScreenBoundsForRoom(room);
        if (bounds != null) {
            int pad = 60;
            Rectangle padded = new Rectangle(
                    Math.max(0, bounds.x - pad),
                    Math.max(0, bounds.y - pad),
                    bounds.width + pad * 2,
                    bounds.height + pad * 2
            );
            floorCanvas.scrollRectToVisible(padded);
        }
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
            isFloorPlanDirty = true;
            isStudentBookingDirty = true;
            isSecretaryDirty = true;
            isAdminDirty = true;
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
