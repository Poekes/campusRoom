package com.campusroom.ui;

import com.campusroom.model.Building;
import com.campusroom.model.Campus;
import com.campusroom.model.Room;
import com.campusroom.service.AvailabilityService;
import com.campusroom.service.MasterDataService;
import com.campusroom.service.ServiceResult;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * LayoutEditorPanel - Fitur Visual Editor Layout Denah Ruangan (Admin Master).
 * Didesain ulang terinspirasi dari canvas tools modern (seperti Excalidraw / Figma):
 * - Floating Island Toolbar di bagian atas (dock pill minimalis dengan tool icon).
 * - Location Selector Island (Gedung & Lantai + Tambah Lantai Baru).
 * - Document & Blueprint Actions Island (Simpan Layout & Blueprint background).
 * - Room Inspector menggunakan sleek Modern Modal Dialog (RoomInspectorDialog).
 * - Zero visual clutter, memaksimalkan area kerja canvas denah interaktif.
 */
public class LayoutEditorPanel extends JPanel implements InteractiveFloorPlanPanel.FloorPlanListener {
    private final MasterDataService masterDataService = new MasterDataService();
    private final AvailabilityService availabilityService = new AvailabilityService();
    private final Runnable onLayoutSavedCallback;

    // Location Controls (Island 1)
    private JComboBox<Campus> cbCampus;
    private JComboBox<Building> cbBuilding;
    private JComboBox<String> cbFloor;
    private JButton btnAddFloor;
    private JButton btnDeleteFloor;

    // Tool Dock Buttons (Island 2)
    private JButton btnToolPointer;
    private JButton btnToolAddRoom;
    private JButton btnToolDuplicate;
    private JButton btnToolGrid;
    private JButton btnToolAlignLeft;
    private JButton btnToolStandardSize;
    private JButton btnToolInspector;
    private JButton btnToolDeleteRoom;
    private JButton btnToolResetView;

    // Document & Blueprint Controls (Island 3)
    private JButton btnBlueprintMenu;
    private JLabel lblBlueprintStatus;
    private JButton btnSave;
    private JLabel lblSaveStatus;
    private JButton btnCancel;

    // Center Canvas
    private InteractiveFloorPlanPanel floorCanvas;
    private JScrollPane canvasScrollPane;

    // Bottom Floating Status Bar
    private JLabel lblFooterStatus;
    private JButton btnFooterInspector;
    private JLabel lblFooterDoc;

    // State
    private List<Room> currentRooms = new ArrayList<>();
    private Room activeRoom;
    private boolean isUpdatingCombos = false;
    private boolean hasUnsavedChanges = false;

    public LayoutEditorPanel(Runnable onLayoutSaved) {
        this.onLayoutSavedCallback = onLayoutSaved;
        setLayout(new BorderLayout());
        setBackground(new Color(241, 245, 249));

        initFloatingHeader();
        initCenterCanvas();
        initFooterBar();
        initKeyboardShortcuts();

        loadCampuses();
    }

    /**
     * Membuat Header mengambang (Floating Island Header ala Excalidraw)
     */
    private void initFloatingHeader() {
        JPanel headerContainer = new JPanel(new BorderLayout(8, 0));
        headerContainer.setBackground(new Color(248, 250, 252));
        headerContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(8, 12, 8, 12)
        ));

        // 1. Island Kiri: Pemilihan Gedung & Lantai
        headerContainer.add(createLocationIsland(), BorderLayout.WEST);

        // 2. Island Tengah: Toolbar Pill Dok Excalidraw
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerWrapper.setOpaque(false);
        centerWrapper.add(createToolDockIsland());
        headerContainer.add(centerWrapper, BorderLayout.CENTER);

        // 3. Island Kanan: Blueprint & Simpan Dokumen
        headerContainer.add(createDocumentIsland(), BorderLayout.EAST);

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.add(headerContainer, BorderLayout.NORTH);
        topWrapper.add(createEditorHintBar(), BorderLayout.SOUTH);

        add(topWrapper, BorderLayout.NORTH);
    }

    private JPanel createEditorHintBar() {
        JPanel hintBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 5));
        hintBar.setBackground(new Color(238, 242, 255));
        hintBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(199, 210, 254)));

        JLabel lblHint = new JLabel("<html><b>Panduan Edit Denah:</b> " +
                "<b>1. Pilih:</b> Klik ruangan &nbsp;•&nbsp; " +
                "<b>2. Pindah Posisi:</b> Drag/geser ruangan &nbsp;•&nbsp; " +
                "<b>3. Ubah Ukuran:</b> Tarik sudut oranye kanan bawah &nbsp;•&nbsp; " +
                "<b>4. Edit Data:</b> Klik ganda ruangan atau tekan <i>Edit Properti</i></html>");
        lblHint.setFont(ModernUIHelper.fontPlain(11));
        lblHint.setForeground(new Color(55, 48, 163));
        hintBar.add(lblHint);
        return hintBar;
    }

    /**
     * Island 1: Pemilihan Kampus, Gedung, dan Lantai
     */
    private JPanel createLocationIsland() {
        JPanel island = createIslandContainer();

        JLabel iconLbl = new JLabel("🏢");
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        island.add(iconLbl);

        cbCampus = new JComboBox<>();
        cbCampus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cbCampus.setPreferredSize(new Dimension(110, 28));
        cbCampus.setToolTipText("Pilih Kampus");
        cbCampus.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Campus) {
                    Campus c = (Campus) value;
                    setText(c.getName().split(" \\(")[0]);
                    setToolTipText(c.getName());
                }
                return this;
            }
        });
        cbCampus.addActionListener(e -> {
            if (!isUpdatingCombos) onCampusChanged();
        });
        island.add(cbCampus);

        cbBuilding = new JComboBox<>();
        cbBuilding.setFont(new Font("SansSerif", Font.BOLD, 11));
        cbBuilding.setPreferredSize(new Dimension(110, 28));
        cbBuilding.setToolTipText("Pilih Gedung");
        cbBuilding.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Building) {
                    Building b = (Building) value;
                    setText(b.getName().split(" \\(")[0]);
                    setToolTipText(b.getName());
                }
                return this;
            }
        });
        cbBuilding.addActionListener(e -> {
            if (!isUpdatingCombos) onBuildingChanged();
        });
        island.add(cbBuilding);

        cbFloor = new JComboBox<>();
        cbFloor.setFont(new Font("SansSerif", Font.BOLD, 11));
        cbFloor.setPreferredSize(new Dimension(85, 28));
        cbFloor.setToolTipText("Pilih Tingkat Lantai");
        cbFloor.addActionListener(e -> {
            if (!isUpdatingCombos) onFloorChanged();
        });
        island.add(cbFloor);

        // Tombol Tambah Tingkat Lantai Baru
        btnAddFloor = createToolButton("+", null, "Tambah tingkat lantai baru ke gedung ini", false, e -> handleAddFloor());
        btnAddFloor.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAddFloor.setPreferredSize(new Dimension(26, 28));
        island.add(btnAddFloor);

        // Tombol Hapus Tingkat Lantai Teratas
        btnDeleteFloor = createToolButton("🗑", null, "Hapus tingkat lantai teratas jika kosong", false, e -> handleDeleteFloor());
        btnDeleteFloor.setPreferredSize(new Dimension(26, 28));
        island.add(btnDeleteFloor);

        return island;
    }

    /**
     * Island 2: Tool Dock Pill Excalidraw (Pointer, Add, Duplicate, Grid, Align, Size, Modal Inspector, Delete, Reset)
     */
    private JPanel createToolDockIsland() {
        JPanel island = createIslandContainer();

        // Tool 1: Pointer / Select Mode
        btnToolPointer = createToolButton("↖", null, "Pointer / Select Mode Objek (V)", true, e -> {
            floorCanvas.setLayoutEditMode(true);
        });
        btnToolPointer.setPreferredSize(new Dimension(28, 28));
        island.add(btnToolPointer);

        // Tool 2: Tambah Ruangan Baru
        btnToolAddRoom = createToolButton("+", null, "Tambah Ruangan Baru pada Lantai Ini (R)", false, e -> showAddRoomDialog());
        btnToolAddRoom.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnToolAddRoom.setPreferredSize(new Dimension(28, 28));
        island.add(btnToolAddRoom);

        // Tool 3: Duplikasi Ruangan
        btnToolDuplicate = createToolButton("⧉", null, "Duplikasi Ruangan Terpilih (D)", false, e -> handleDuplicateRoom());
        btnToolDuplicate.setPreferredSize(new Dimension(28, 28));
        island.add(btnToolDuplicate);

        island.add(createToolSeparator());

        // Tool 4: Auto Grid
        btnToolGrid = createToolButton("▦", null, "Tata Ulang Posisi Seluruh Ruangan ke Grid (G)", false, e -> showGridMenu());
        btnToolGrid.setPreferredSize(new Dimension(28, 28));
        island.add(btnToolGrid);

        // Tool 5: Rata Kiri
        btnToolAlignLeft = createToolButton("⇤", null, "Ratakan Ruangan ke Margin Kiri (X = 60)", false, e -> handleAlignLeft());
        btnToolAlignLeft.setPreferredSize(new Dimension(28, 28));
        island.add(btnToolAlignLeft);

        // Tool 6: Standar Ukuran
        btnToolStandardSize = createToolButton("⤢", null, "Standarkan Ukuran Ruangan ke 180 × 150 px", false, e -> handleStandardizeSize());
        btnToolStandardSize.setPreferredSize(new Dimension(28, 28));
        island.add(btnToolStandardSize);

        island.add(createToolSeparator());

        // Tool 7: Edit Properti Ruangan (Modal Dialog Properti & Dimensi)
        btnToolInspector = createToolButton("⚙", "Edit", "Buka Dialog Edit Data & Ukuran Ruangan (P / Enter)", true, e -> openRoomInspectorModal(activeRoom));
        btnToolInspector.setBackground(new Color(238, 242, 255)); // Indigo 50
        btnToolInspector.setForeground(new Color(79, 70, 229));    // Indigo 600
        btnToolInspector.setPreferredSize(new Dimension(56, 28));
        island.add(btnToolInspector);

        // Tool 8: Hapus Ruangan
        btnToolDeleteRoom = createToolButton("🗑", null, "Hapus Ruangan Terpilih (Del)", false, e -> handleDeleteRoom());
        btnToolDeleteRoom.setForeground(new Color(220, 38, 38)); // Red 600
        btnToolDeleteRoom.setPreferredSize(new Dimension(28, 28));
        island.add(btnToolDeleteRoom);

        island.add(createToolSeparator());

        // Tool 9: Reset Scroll / View
        btnToolResetView = createToolButton("⌖", null, "Reset Scroll Denah ke Posisi Awal", false, e -> {
            if (canvasScrollPane != null) {
                canvasScrollPane.getHorizontalScrollBar().setValue(0);
                canvasScrollPane.getVerticalScrollBar().setValue(0);
            }
        });
        btnToolResetView.setPreferredSize(new Dimension(28, 28));
        island.add(btnToolResetView);

        // Tool 10: Putar Denah 90° (POV)
        JButton btnToolRotate = createToolButton("↻", null, "Putar Denah 90° Searah Jarum Jam (POV)", false, e -> {
            if (floorCanvas != null) {
                floorCanvas.rotateClockwise();
            }
        });
        btnToolRotate.setPreferredSize(new Dimension(28, 28));
        island.add(btnToolRotate);

        return island;
    }

    /**
     * Island 3: Blueprint & Aksi Simpan Dokumen
     */
    private JPanel createDocumentIsland() {
        JPanel island = createIslandContainer();

        // Menu Blueprint
        btnBlueprintMenu = createToolButton("🗺️", "Blueprint ▾", "Kelola Gambar Denah Blueprint Lantai Ini", false, e -> showBlueprintMenu());
        island.add(btnBlueprintMenu);

        lblBlueprintStatus = new JLabel("Standar");
        lblBlueprintStatus.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblBlueprintStatus.setOpaque(true);
        lblBlueprintStatus.setBackground(new Color(241, 245, 249));
        lblBlueprintStatus.setForeground(new Color(100, 116, 139));
        lblBlueprintStatus.setBorder(new EmptyBorder(3, 5, 3, 5));
        island.add(lblBlueprintStatus);

        island.add(createToolSeparator());

        // Status Simpan Pill
        lblSaveStatus = new JLabel("✓ Tersimpan");
        lblSaveStatus.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblSaveStatus.setOpaque(true);
        lblSaveStatus.setBackground(new Color(220, 252, 231)); // Emerald 100
        lblSaveStatus.setForeground(new Color(21, 128, 61));    // Emerald 700
        lblSaveStatus.setBorder(new EmptyBorder(4, 7, 4, 7));
        island.add(lblSaveStatus);

        // Tombol Simpan Layout
        btnSave = new JButton("💾 Simpan") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isPressed() ? new Color(5, 150, 105) : (getModel().isRollover() ? new Color(16, 185, 129).darker() : new Color(16, 185, 129));
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSave.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setContentAreaFilled(false);
        btnSave.setOpaque(false);
        btnSave.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSave.setBorder(new EmptyBorder(5, 9, 5, 9));
        btnSave.setToolTipText("Simpan seluruh perubahan koordinat layout ke database permanen (Ctrl+S)");
        btnSave.addActionListener(e -> handleSaveLayoutToDb());
        island.add(btnSave);

        // Tombol Batalkan / Muat Ulang
        btnCancel = createToolButton("↩️", null, "Muat ulang data denah dari database & batalkan perubahan", false, e -> onFloorChanged());
        btnCancel.setPreferredSize(new Dimension(26, 28));
        island.add(btnCancel);

        return island;
    }

    private JPanel createIslandContainer() {
        JPanel island = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Drop shadow halus
                g2.setColor(new Color(0, 0, 0, 14));
                g2.fillRoundRect(2, 2, w - 4, h - 3, 10, 10);

                // Latar putih kartu
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 2, h - 2, 10, 10);

                // Garis batas halus
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 2, h - 2, 10, 10);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        island.setOpaque(false);
        island.setBorder(new EmptyBorder(2, 6, 2, 6));
        return island;
    }

    private JButton createToolButton(String icon, String text, String tooltip, boolean isAccent, ActionListener listener) {
        String label = (text != null && !text.isEmpty()) ? icon + " " + text : icon;
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = getBackground();
                if (!isEnabled()) {
                    bg = new Color(241, 245, 249);
                } else if (getModel().isPressed()) {
                    bg = isAccent ? new Color(224, 231, 255) : new Color(226, 232, 240);
                } else if (getModel().isRollover()) {
                    bg = isAccent ? new Color(238, 242, 255) : new Color(241, 245, 249);
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                g2.setColor(isAccent ? new Color(199, 210, 254) : new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("SansSerif", isAccent ? Font.BOLD : Font.PLAIN, 11));
        btn.setForeground(isAccent ? new Color(67, 56, 202) : new Color(30, 41, 59));
        btn.setBackground(isAccent ? new Color(245, 247, 255) : Color.WHITE);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(4, 6, 4, 6));
        if (listener != null) {
            btn.addActionListener(listener);
        }
        return btn;
    }

    private JSeparator createToolSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 20));
        sep.setForeground(new Color(226, 232, 240));
        return sep;
    }

    private void initCenterCanvas() {
        floorCanvas = new InteractiveFloorPlanPanel();
        floorCanvas.setLayoutEditMode(true);
        floorCanvas.setFloorPlanListener(this);

        canvasScrollPane = new JScrollPane(floorCanvas);
        canvasScrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        canvasScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        canvasScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        add(canvasScrollPane, BorderLayout.CENTER);
    }

    private void initFooterBar() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                new EmptyBorder(6, 16, 6, 16)
        ));

        // Kiri: Status pilihan & Tombol Buka Modal Inspector
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        lblFooterStatus = new JLabel("💡 Navigasi Canvas: Klik ruangan untuk memilih • Tarik sudut kanan-bawah untuk resize • Double-click ruangan untuk buka Properti Ruangan (Modal)");
        lblFooterStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblFooterStatus.setForeground(new Color(71, 85, 105));
        leftPanel.add(lblFooterStatus);

        btnFooterInspector = new JButton("⚙ Edit Data & Ukuran Ruangan");
        btnFooterInspector.setFont(new Font("SansSerif", Font.BOLD, 10));
        btnFooterInspector.setForeground(new Color(79, 70, 229));
        btnFooterInspector.setBackground(new Color(238, 242, 255));
        btnFooterInspector.setFocusPainted(false);
        btnFooterInspector.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnFooterInspector.setVisible(false);
        btnFooterInspector.addActionListener(e -> openRoomInspectorModal(activeRoom));
        leftPanel.add(btnFooterInspector);

        footer.add(leftPanel, BorderLayout.WEST);

        // Kanan: Info Dokumen & Zoom
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        lblFooterDoc = new JLabel("Memuat data...");
        lblFooterDoc.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblFooterDoc.setForeground(new Color(100, 116, 139));
        rightPanel.add(lblFooterDoc);

        JLabel lblScale = new JLabel("100%");
        lblScale.setFont(ModernUIHelper.fontBold(10));
        lblScale.setForeground(ModernUIHelper.COLOR_PRIMARY);
        lblScale.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblScale.setToolTipText("Klik untuk reset zoom (100%)");
        lblScale.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (floorCanvas != null) floorCanvas.resetZoom();
            }
        });
        floorCanvas.addZoomChangeListener(zoom -> {
            lblScale.setText((int) Math.round(zoom * 100) + "%");
        });
        rightPanel.add(lblScale);

        footer.add(rightPanel, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private void initKeyboardShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        // Delete Room
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteRoom");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteRoom");
        am.put("deleteRoom", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (activeRoom != null) handleDeleteRoom();
            }
        });

        // Inspector Modal
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "openInspector");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openInspector");
        am.put("openInspector", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openRoomInspectorModal(activeRoom);
            }
        });

        // Duplicate
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "duplicateRoom");
        am.put("duplicateRoom", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (activeRoom != null) handleDuplicateRoom();
            }
        });

        // Add Room
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "addRoom");
        am.put("addRoom", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddRoomDialog();
            }
        });

        // Save (Ctrl+S / Cmd+S)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "saveLayout");
        am.put("saveLayout", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSaveLayoutToDb();
            }
        });
    }

    private void markUnsavedChanges(boolean unsaved) {
        this.hasUnsavedChanges = unsaved;
        if (unsaved) {
            lblSaveStatus.setText("● Belum Disimpan");
            lblSaveStatus.setBackground(new Color(254, 243, 199)); // Amber 100
            lblSaveStatus.setForeground(new Color(180, 83, 9));    // Amber 700
        } else {
            lblSaveStatus.setText("✓ Tersimpan");
            lblSaveStatus.setBackground(new Color(220, 252, 231)); // Emerald 100
            lblSaveStatus.setForeground(new Color(21, 128, 61));   // Emerald 700
        }
    }

    /**
     * Membuka Sleek Modern Modal Dialog untuk Ruang Terpilih
     */
    public RoomInspectorDialog openRoomInspectorModal(Room room) {
        if (room == null) {
            if (!currentRooms.isEmpty()) {
                room = currentRooms.get(0);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Belum ada ruangan pada lantai ini.\nKlik tombol '➕' di toolbar atas untuk menambahkan ruangan baru.",
                        "Informasi", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
        }
        onRoomSelected(room);

        Window parentWin = SwingUtilities.getWindowAncestor(this);
        Building b = (Building) cbBuilding.getSelectedItem();
        int floorNum = getSelectedFloorNumber();

        final Room targetRoom = room;
        RoomInspectorDialog dialog = new RoomInspectorDialog(
                parentWin, targetRoom, b, floorNum,
                () -> {
                    // On Save
                    markUnsavedChanges(false);
                    floorCanvas.repaint();
                    onRoomSelected(targetRoom);
                    if (onLayoutSavedCallback != null) onLayoutSavedCallback.run();
                },
                () -> {
                    // On Delete
                    onFloorChanged();
                    if (onLayoutSavedCallback != null) onLayoutSavedCallback.run();
                },
                () -> {
                    // On Duplicate
                    handleDuplicateRoom();
                }
        );
        dialog.setVisible(true);
        return dialog;
    }

    private void showGridMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem g4 = new JMenuItem("▦ Auto Grid: 4 Kolom (Standar)");
        g4.addActionListener(e -> autoArrangeGrid(4));
        JMenuItem g2 = new JMenuItem("▦ Auto Grid: 2 Kolom");
        g2.addActionListener(e -> autoArrangeGrid(2));
        JMenuItem g3 = new JMenuItem("▦ Auto Grid: 3 Kolom");
        g3.addActionListener(e -> autoArrangeGrid(3));

        menu.add(g4);
        menu.add(g2);
        menu.add(g3);
        menu.show(btnToolGrid, 0, btnToolGrid.getHeight());
    }

    private void showBlueprintMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem itemUpload = new JMenuItem("📁 Unggah Blueprint Lantai Ini (PNG/JPG)...");
        itemUpload.addActionListener(e -> handleUploadBlueprint());

        JMenuItem itemReset = new JMenuItem("🔄 Gunakan Blueprint Standar");
        itemReset.addActionListener(e -> handleResetBlueprint());

        menu.add(itemUpload);
        menu.add(itemReset);
        menu.show(btnBlueprintMenu, 0, btnBlueprintMenu.getHeight());
    }

    // --- Data Loading & Event Handlers ---

    public void loadCampuses() {
        isUpdatingCombos = true;
        cbCampus.removeAllItems();
        List<Campus> campuses = masterDataService.getAllCampuses();
        for (Campus c : campuses) {
            cbCampus.addItem(c);
        }
        isUpdatingCombos = false;

        if (!campuses.isEmpty()) {
            cbCampus.setSelectedIndex(0);
            onCampusChanged();
        }
    }

    private void onCampusChanged() {
        Campus c = (Campus) cbCampus.getSelectedItem();
        if (c == null) return;

        isUpdatingCombos = true;
        cbBuilding.removeAllItems();
        List<Building> buildings = masterDataService.getBuildingsByCampus(c.getId());
        for (Building b : buildings) {
            cbBuilding.addItem(b);
        }
        isUpdatingCombos = false;

        if (!buildings.isEmpty()) {
            cbBuilding.setSelectedIndex(0);
            onBuildingChanged();
        }
    }

    private void onBuildingChanged() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;

        isUpdatingCombos = true;
        cbFloor.removeAllItems();
        int totalFloors = Math.max(1, b.getTotalFloors());
        for (int i = 1; i <= totalFloors; i++) {
            cbFloor.addItem("Lantai " + i);
        }

        // Default pilih Lantai 2 jika Gedung A, selain itu Lantai 1
        if (b.getId() == 1 && totalFloors >= 2) {
            cbFloor.setSelectedIndex(1); // Lantai 2
        } else {
            cbFloor.setSelectedIndex(0);
        }
        isUpdatingCombos = false;

        onFloorChanged();
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
        isUpdatingCombos = true;
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
        isUpdatingCombos = false;
        onFloorChanged();
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

    public void onFloorChanged() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;

        int floorNum = getSelectedFloorNumber();

        // 1. Muat Blueprint Gambar Denah untuk Gedung & Lantai Ini via MasterDataService
        String imgPath = masterDataService.getFloorPlanImage(b.getId(), floorNum);
        floorCanvas.loadFloorImage(imgPath);

        if (imgPath != null && !imgPath.isEmpty()) {
            File f = new File(imgPath);
            lblBlueprintStatus.setText("Kustom");
            lblBlueprintStatus.setToolTipText("File blueprint aktif: " + f.getName());
            btnBlueprintMenu.setToolTipText("Blueprint denah: " + f.getName());
            lblBlueprintStatus.setBackground(new Color(224, 242, 254));
            lblBlueprintStatus.setForeground(new Color(3, 105, 161));
        } else {
            lblBlueprintStatus.setText("Standar");
            lblBlueprintStatus.setToolTipText("Menggunakan blueprint standar bawaan");
            btnBlueprintMenu.setToolTipText("Blueprint denah: Standar");
            lblBlueprintStatus.setBackground(new Color(241, 245, 249));
            lblBlueprintStatus.setForeground(new Color(100, 116, 139));
        }

        // 2. Muat Ruangan khusus untuk Gedung & Lantai Ini via MasterDataService dan sinkronkan availability real-time
        currentRooms = masterDataService.getRoomsByBuildingAndFloor(b.getId(), floorNum);
        availabilityService.updateRoomsAvailability(currentRooms, LocalDate.now(), "08:00", "12:00");
        floorCanvas.setRooms(currentRooms);

        if (!currentRooms.isEmpty()) {
            onRoomSelected(currentRooms.get(0));
        } else {
            activeRoom = null;
            updateSelectionUI();
        }

        lblFooterDoc.setText(b.getName() + " • Lantai " + floorNum + " (" + currentRooms.size() + " Ruangan)");
        markUnsavedChanges(false);
    }

    private void updateSelectionUI() {
        boolean hasRoom = (activeRoom != null);
        btnToolDuplicate.setEnabled(hasRoom);
        btnToolAlignLeft.setEnabled(hasRoom);
        btnToolStandardSize.setEnabled(hasRoom);
        btnToolInspector.setEnabled(hasRoom || !currentRooms.isEmpty());
        btnToolDeleteRoom.setEnabled(hasRoom);

        if (hasRoom) {
            lblFooterStatus.setText("Terpilih: " + activeRoom.getCode() + " — " + activeRoom.getName() +
                    " (" + activeRoom.getWidth() + "×" + activeRoom.getHeight() + " px) @ (" + activeRoom.getPosX() + ", " + activeRoom.getPosY() + ")");
            btnFooterInspector.setVisible(true);
        } else {
            lblFooterStatus.setText("Klik ruangan untuk memilih • Tarik sudut untuk resize • Klik ganda untuk properti");
            btnFooterInspector.setVisible(false);
        }
    }

    /**
     * Tambah tingkat lantai baru pada gedung
     */
    private void handleAddFloor() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;

        int currentMax = Math.max(1, b.getTotalFloors());
        int nextFloor = currentMax + 1;

        String input = JOptionPane.showInputDialog(this,
                "Nomor lantai baru:",
                String.valueOf(nextFloor));
        if (input == null || input.trim().isEmpty()) return;

        try {
            int newFloorNum = Integer.parseInt(input.trim());
            if (newFloorNum <= 0) {
                JOptionPane.showMessageDialog(this, "Nomor lantai harus lebih dari 0.", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int newTotal = Math.max(b.getTotalFloors(), newFloorNum);
            ServiceResult<Void> ok = masterDataService.updateTotalFloors(b.getId(), newTotal);
            if (ok.isSuccess()) {
                b.setTotalFloors(newTotal);

                // Buat blueprint bawaan untuk lantai baru jika belum ada via MasterDataService
                String defaultImg = (b.getId() == 3) ? "images/kampus2_lantai_1.png" : ((b.getId() == 2) ? "images/floorplan_gedung_b.png" : "images/floorplan_gedung_a.png");
                masterDataService.saveFloorPlanImage(b.getId(), newFloorNum, defaultImg);

                // Perbarui dropdown lantai
                isUpdatingCombos = true;
                cbFloor.removeAllItems();
                for (int i = 1; i <= newTotal; i++) {
                    cbFloor.addItem("Lantai " + i);
                }
                cbFloor.setSelectedItem("Lantai " + newFloorNum);
                isUpdatingCombos = false;

                onFloorChanged();

                JOptionPane.showMessageDialog(this,
                        "Lantai " + newFloorNum + " berhasil ditambahkan.",
                        "Sukses", JOptionPane.INFORMATION_MESSAGE);

                if (onLayoutSavedCallback != null) onLayoutSavedCallback.run();
            } else {
                JOptionPane.showMessageDialog(this, ok.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Nomor lantai harus berupa angka.", "Peringatan", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleDeleteFloor() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;
        int currentMax = Math.max(1, b.getTotalFloors());
        if (currentMax <= 1) {
            JOptionPane.showMessageDialog(this, "Gedung minimal memiliki 1 lantai.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Room> roomsOnTopFloor = masterDataService.getRoomsByBuildingAndFloor(b.getId(), currentMax);
        if (!roomsOnTopFloor.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Lantai " + currentMax + " masih memiliki " + roomsOnTopFloor.size() + " ruangan.\nHapus semua ruangan terlebih dahulu.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Hapus Lantai " + currentMax + "?",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            int newTotal = currentMax - 1;
            ServiceResult<Void> ok = masterDataService.updateTotalFloors(b.getId(), newTotal);
            if (ok.isSuccess()) {
                b.setTotalFloors(newTotal);
                isUpdatingCombos = true;
                cbFloor.removeAllItems();
                for (int i = 1; i <= newTotal; i++) {
                    cbFloor.addItem("Lantai " + i);
                }
                cbFloor.setSelectedIndex(newTotal - 1);
                isUpdatingCombos = false;
                onFloorChanged();
                JOptionPane.showMessageDialog(this, "Lantai " + currentMax + " berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                if (onLayoutSavedCallback != null) onLayoutSavedCallback.run();
            } else {
                JOptionPane.showMessageDialog(this, ok.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleUploadBlueprint() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;
        int floorNum = getSelectedFloorNumber();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih Blueprint (" + b.getName() + " Lantai " + floorNum + ")");
        int res = fileChooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();

            ServiceResult<Void> ok = masterDataService.saveFloorPlanImage(b.getId(), floorNum, path);
            if (ok.isSuccess()) {
                floorCanvas.loadFloorImage(path);
                lblBlueprintStatus.setText("Kustom");
                lblBlueprintStatus.setToolTipText("File: " + selectedFile.getName());
                btnBlueprintMenu.setToolTipText("Blueprint: " + selectedFile.getName());
                lblBlueprintStatus.setBackground(new Color(224, 242, 254));
                lblBlueprintStatus.setForeground(new Color(3, 105, 161));
                JOptionPane.showMessageDialog(this,
                        "Blueprint berhasil diperbarui.",
                        "Sukses",
                        JOptionPane.INFORMATION_MESSAGE);
                if (onLayoutSavedCallback != null) onLayoutSavedCallback.run();
            } else {
                JOptionPane.showMessageDialog(this, ok.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleResetBlueprint() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;
        int floorNum = getSelectedFloorNumber();

        String defaultImg = (b.getId() == 3)
                ? (floorNum == 2 ? "images/kampus2_lantai_2.png" : (floorNum == 3 ? "images/kampus2_lantai_3.png" : "images/kampus2_lantai_1.png"))
                : ((b.getId() == 2) ? "images/floorplan_gedung_b.png" : "images/floorplan_gedung_a.png");
        masterDataService.saveFloorPlanImage(b.getId(), floorNum, defaultImg);
        floorCanvas.loadFloorImage(defaultImg);
        lblBlueprintStatus.setText("Standar");
        lblBlueprintStatus.setToolTipText("Blueprint standar");
        btnBlueprintMenu.setToolTipText("Blueprint: Standar");
        lblBlueprintStatus.setBackground(new Color(241, 245, 249));
        lblBlueprintStatus.setForeground(new Color(100, 116, 139));
        JOptionPane.showMessageDialog(this, "Blueprint direset ke standar.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
        if (onLayoutSavedCallback != null) onLayoutSavedCallback.run();
    }

    private void autoArrangeGrid(int columns) {
        if (currentRooms == null || currentRooms.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tidak ada ruangan di lantai ini untuk ditata.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int startX = 60;
        int startY = 70;
        int rw = 180;
        int rh = 150;
        int gapX = 20;
        int gapY = 270;

        for (int i = 0; i < currentRooms.size(); i++) {
            Room r = currentRooms.get(i);
            int row = i / columns;
            int col = i % columns;

            r.setPosX(startX + col * (rw + gapX));
            r.setPosY(startY + row * gapY);
            r.setWidth(rw);
            r.setHeight(rh);
        }

        markUnsavedChanges(true);
        floorCanvas.repaint();
        if (activeRoom != null) {
            onRoomSelected(activeRoom);
        }
    }

    private void handleAlignLeft() {
        if (activeRoom == null) {
            JOptionPane.showMessageDialog(this, "Pilih salah satu ruangan terlebih dahulu!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        activeRoom.setPosX(60);
        onRoomSelected(activeRoom);
        markUnsavedChanges(true);
        floorCanvas.repaint();
    }

    private void handleStandardizeSize() {
        if (activeRoom == null) {
            JOptionPane.showMessageDialog(this, "Pilih salah satu ruangan terlebih dahulu!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        activeRoom.setWidth(180);
        activeRoom.setHeight(150);
        onRoomSelected(activeRoom);
        markUnsavedChanges(true);
        floorCanvas.repaint();
    }

    private void showAddRoomDialog() {
        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;
        int floorNum = getSelectedFloorNumber();

        // Rekomendasi kode ruangan pintar berdasarkan lantai
        String suggestedCode = "R. " + floorNum + String.format("%02d", currentRooms.size() + 1);

        JTextField txtCode = new JTextField(suggestedCode, 12);
        JTextField txtName = new JTextField(20);
        JSpinner spCap = new JSpinner(new SpinnerNumberModel(40, 5, 500, 5));
        String[] types = {"Kelas Teori", "Smart Classroom", "Laboratorium", "Studio", "Auditorium", "Ruang Seminar", "Administrasi"};
        JComboBox<String> cbType = new JComboBox<>(types);
        JTextField txtFac = new JTextField("LCD Proyektor, AC, Whiteboard", 20);

        JPanel pnl = new JPanel(new GridLayout(5, 2, 6, 6));
        pnl.add(new JLabel("Kode Ruangan:")); pnl.add(txtCode);
        pnl.add(new JLabel("Nama Ruangan:")); pnl.add(txtName);
        pnl.add(new JLabel("Kapasitas Kursi:")); pnl.add(spCap);
        pnl.add(new JLabel("Tipe Ruangan:")); pnl.add(cbType);
        pnl.add(new JLabel("Fasilitas:")); pnl.add(txtFac);

        int res = JOptionPane.showConfirmDialog(this, pnl, "Tambah Ruangan Baru — " + b.getName() + " Lt. " + floorNum, JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String code = txtCode.getText().trim();
            String name = txtName.getText().trim();
            if (code.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kode dan Nama Ruangan wajib diisi!", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Check duplicate code via MasterDataService
            if (!masterDataService.isRoomCodeUnique(b.getId(), 0, code)) {
                JOptionPane.showMessageDialog(this,
                        "Kode ruangan '" + code + "' sudah digunakan oleh ruangan lain di gedung ini!\nSilakan gunakan kode ruangan yang unik.",
                        "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int imgW = (floorCanvas != null && floorCanvas.getFloorImage() != null) ? floorCanvas.getFloorImage().getWidth() : 1100;
            int imgH = (floorCanvas != null && floorCanvas.getFloorImage() != null) ? floorCanvas.getFloorImage().getHeight() : 650;
            int posX = Math.min(imgW - 200, Math.max(60, 60 + (currentRooms.size() % 5) * 190));
            int posY = Math.min(imgH - 170, Math.max(70, 70 + ((currentRooms.size() / 5) % 3) * 170));
            Room newRoom = new Room(b.getId(), code, name, floorNum, (Integer) spCap.getValue(),
                    (String) cbType.getSelectedItem(), txtFac.getText().trim(), posX, posY, 180, 150);

            ServiceResult<Room> addRes = masterDataService.addRoom(newRoom);
            if (addRes.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Ruangan " + code + " berhasil ditambahkan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                onFloorChanged();
                if (onLayoutSavedCallback != null) onLayoutSavedCallback.run();
            } else {
                JOptionPane.showMessageDialog(this, addRes.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDuplicateRoom() {
        if (activeRoom == null) {
            JOptionPane.showMessageDialog(this, "Pilih salah satu ruangan yang ingin diduplikasi terlebih dahulu!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Building b = (Building) cbBuilding.getSelectedItem();
        if (b == null) return;
        int floorNum = getSelectedFloorNumber();

        String newCode = JOptionPane.showInputDialog(this,
                "Kode ruangan baru:",
                activeRoom.getCode() + "-COPY");
        if (newCode == null || newCode.trim().isEmpty()) return;

        // Check duplicate code via MasterDataService
        if (!masterDataService.isRoomCodeUnique(b.getId(), 0, newCode.trim())) {
            JOptionPane.showMessageDialog(this,
                    "Kode ruangan '" + newCode.trim() + "' sudah digunakan.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int newX = Math.min(1000, activeRoom.getPosX() + 30);
        int newY = Math.min(700, activeRoom.getPosY() + 30);

        Room dup = new Room(
                b.getId(),
                newCode.trim(),
                activeRoom.getName() + " (Salinan)",
                floorNum,
                activeRoom.getCapacity(),
                activeRoom.getRoomType(),
                activeRoom.getFacilities(),
                newX,
                newY,
                activeRoom.getWidth(),
                activeRoom.getHeight()
        );

        ServiceResult<Room> dupRes = masterDataService.addRoom(dup);
        if (dupRes.isSuccess()) {
            JOptionPane.showMessageDialog(this, "Ruangan " + newCode.trim() + " berhasil diduplikasi.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            onFloorChanged();
            if (onLayoutSavedCallback != null) onLayoutSavedCallback.run();
        } else {
            JOptionPane.showMessageDialog(this, dupRes.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteRoom() {
        if (activeRoom == null) {
            JOptionPane.showMessageDialog(this, "Pilih ruangan terlebih dahulu.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Hapus ruangan " + activeRoom.getCode() + "?",
                "Hapus Ruangan",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            ServiceResult<Void> delRes = masterDataService.deleteRoom(activeRoom.getId());
            if (delRes.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Ruangan berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                onFloorChanged();
                if (onLayoutSavedCallback != null) onLayoutSavedCallback.run();
            } else {
                JOptionPane.showMessageDialog(this, delRes.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleSaveLayoutToDb() {
        if (currentRooms == null || currentRooms.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tidak ada ruangan untuk disimpan.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Building b = (Building) cbBuilding.getSelectedItem();
        int floorNum = getSelectedFloorNumber();

        boolean allOk = true;
        for (Room r : currentRooms) {
            ServiceResult<Void> ok = masterDataService.updateRoomCoordinates(r.getId(), r.getPosX(), r.getPosY(), r.getWidth(), r.getHeight());
            if (!ok.isSuccess()) allOk = false;
        }

        if (allOk) {
            markUnsavedChanges(false);
            JOptionPane.showMessageDialog(this,
                    "Layout denah berhasil disimpan.",
                    "Sukses",
                    JOptionPane.INFORMATION_MESSAGE);

            if (onLayoutSavedCallback != null) {
                onLayoutSavedCallback.run();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan koordinat ruangan.", "Kesalahan", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- InteractiveFloorPlanPanel Callbacks ---

    @Override
    public void onRoomSelected(Room room) {
        this.activeRoom = room;
        floorCanvas.setSelectedRoom(activeRoom);
        updateSelectionUI();
    }

    @Override
    public void onRoomDoubleClicked(Room room) {
        onRoomSelected(room);
        openRoomInspectorModal(room);
    }

    @Override
    public void onRoomMoved(Room room, int newX, int newY) {
        if (activeRoom == room) {
            markUnsavedChanges(true);
            updateSelectionUI();
        }
    }
}
