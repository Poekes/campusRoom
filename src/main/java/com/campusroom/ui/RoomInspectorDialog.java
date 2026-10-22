package com.campusroom.ui;

import com.campusroom.dao.RoomDao;
import com.campusroom.model.Building;
import com.campusroom.model.Room;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * RoomInspectorDialog - Sleek, modern modal dialog for inspecting and editing room properties
 * and floor plan canvas layout coordinates (inspired by modern canvas tools like Excalidraw / Figma).
 * Replaces fixed permanent right panels to give 100% full screen real estate to the canvas.
 */
public class RoomInspectorDialog extends JDialog {
    private final Room room;
    private final Building building;
    private final int floor;
    private final RoomDao roomDao = new RoomDao();
    private final Runnable onSaveCallback;
    private final Runnable onDeleteCallback;
    private final Runnable onDuplicateCallback;

    private JTextField txtCode;
    private JTextField txtName;
    private JComboBox<String> cbType;
    private JSpinner spCapacity;
    private JTextField txtFacilities;

    private JSpinner spX;
    private JSpinner spY;
    private JSpinner spWidth;
    private JSpinner spHeight;
    private JPanel previewRect;

    public RoomInspectorDialog(Window parent, Room room, Building building, int floor,
                               Runnable onSaveCallback, Runnable onDeleteCallback, Runnable onDuplicateCallback) {
        super(parent, "Properti Ruangan — " + (room != null ? room.getCode() : "Ruangan"), ModalityType.APPLICATION_MODAL);
        this.room = room;
        this.building = building;
        this.floor = floor;
        this.onSaveCallback = onSaveCallback;
        this.onDeleteCallback = onDeleteCallback;
        this.onDuplicateCallback = onDuplicateCallback;

        if (room == null) {
            dispose();
            return;
        }

        initComponents();
        initShortcuts();
        pack();
        setMinimumSize(new Dimension(680, 520));
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(248, 250, 252));

        // 1. Sleek Modern Header
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Body: Two clean side-by-side cards
        JPanel bodyPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        bodyPanel.setOpaque(false);
        bodyPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        bodyPanel.add(createRoomDataCard());
        bodyPanel.add(createCanvasGeometryCard());

        add(bodyPanel, BorderLayout.CENTER);

        // 3. Footer Bar
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private void initShortcuts() {
        // Escape to Cancel / Close
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Ctrl+S / Cmd+S to Save
        getRootPane().registerKeyboardAction(e -> handleSave(),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(new Color(15, 23, 42)); // Slate Navy 900
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Left info
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel titleLbl = new JLabel("⚙ Properti & Dimensi Ruangan");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(Color.WHITE);

        String bName = (building != null) ? building.getName() : "Gedung";
        JLabel subLbl = new JLabel(bName + " • Lantai " + floor + " — [" + room.getCode() + ": " + room.getName() + "]");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLbl.setForeground(new Color(148, 163, 184)); // Slate 400

        left.add(titleLbl);
        left.add(Box.createVerticalStrut(3));
        left.add(subLbl);
        header.add(left, BorderLayout.WEST);

        // Right status indicator pill
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        right.setOpaque(false);

        boolean isAvail = room.isAvailable();
        Color bgBadge = isAvail ? new Color(6, 95, 70) : new Color(153, 27, 27);
        Color fgBadge = isAvail ? new Color(167, 243, 208) : new Color(254, 202, 202);
        String statusText = isAvail ? "● TERSEDIA (Bebas Penggunaan)" : "■ TERPAKAI (Sedang Digunakan)";

        JLabel badgeLbl = new JLabel(statusText);
        badgeLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        badgeLbl.setOpaque(true);
        badgeLbl.setBackground(bgBadge);
        badgeLbl.setForeground(fgBadge);
        badgeLbl.setBorder(new EmptyBorder(5, 12, 5, 12));
        right.add(badgeLbl);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createRoomDataCard() {
        JPanel card = createCardContainer("🏢 INFORMASI RUANGAN");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 6, 4);

        int row = 0;

        // Kode Ruangan
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblCode = new JLabel("Kode Ruang:");
        lblCode.setFont(new Font("SansSerif", Font.BOLD, 11));
        form.add(lblCode, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtCode = new JTextField(room.getCode());
        txtCode.setFont(new Font("SansSerif", Font.BOLD, 12));
        txtCode.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updatePreview(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updatePreview(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        });
        txtCode.addActionListener(e -> handleSave());
        form.add(txtCode, gbc);

        // Nama Ruangan
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblName = new JLabel("Nama Ruangan:");
        lblName.setFont(new Font("SansSerif", Font.BOLD, 11));
        form.add(lblName, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtName = new JTextField(room.getName());
        txtName.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtName.addActionListener(e -> handleSave());
        form.add(txtName, gbc);

        // Tipe Ruangan
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblType = new JLabel("Tipe Ruangan:");
        lblType.setFont(new Font("SansSerif", Font.PLAIN, 11));
        form.add(lblType, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        String[] types = {"Kelas Teori", "Smart Classroom", "Laboratorium", "Studio", "Auditorium", "Ruang Seminar", "Administrasi"};
        cbType = new JComboBox<>(types);
        cbType.setSelectedItem(room.getRoomType());
        cbType.setFont(new Font("SansSerif", Font.PLAIN, 11));
        form.add(cbType, gbc);

        // Kapasitas Kursi
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblCap = new JLabel("Kapasitas (Kursi):");
        lblCap.setFont(new Font("SansSerif", Font.PLAIN, 11));
        form.add(lblCap, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        spCapacity = new JSpinner(new SpinnerNumberModel(room.getCapacity(), 1, 1000, 5));
        spCapacity.setFont(new Font("SansSerif", Font.PLAIN, 11));
        form.add(spCapacity, gbc);

        // Fasilitas
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblFac = new JLabel("Fasilitas:");
        lblFac.setFont(new Font("SansSerif", Font.PLAIN, 11));
        form.add(lblFac, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtFacilities = new JTextField(room.getFacilities());
        txtFacilities.setFont(new Font("SansSerif", Font.PLAIN, 11));
        txtFacilities.addActionListener(e -> handleSave());
        form.add(txtFacilities, gbc);

        // Occupant Info
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 4, 4, 4);
        String occInfo = (room.getCurrentOccupantInfo() != null && !room.getCurrentOccupantInfo().isEmpty())
                ? room.getCurrentOccupantInfo()
                : (room.isAvailable() ? "Bebas reservasi hari ini" : "Terjadwal");
        JLabel lblOcc = new JLabel("<html><i style='color:#64748b; font-size:10px;'>Status terkini: " + occInfo + "</i></html>");
        form.add(lblOcc, gbc);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCanvasGeometryCard() {
        JPanel card = createCardContainer("📐 KOORDINAT & DIMENSI CANVAS");
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);

        // Grid of 4 spinners
        JPanel spinGrid = new JPanel(new GridLayout(2, 4, 8, 8));
        spinGrid.setOpaque(false);

        spX = new JSpinner(new SpinnerNumberModel(room.getPosX(), 0, 3000, 10));
        spY = new JSpinner(new SpinnerNumberModel(room.getPosY(), 0, 3000, 10));
        spWidth = new JSpinner(new SpinnerNumberModel(room.getWidth(), 50, 1000, 10));
        spHeight = new JSpinner(new SpinnerNumberModel(room.getHeight(), 40, 800, 10));

        spX.addChangeListener(e -> updatePreview());
        spY.addChangeListener(e -> updatePreview());
        spWidth.addChangeListener(e -> updatePreview());
        spHeight.addChangeListener(e -> updatePreview());

        JLabel lX = new JLabel("Posisi X:", JLabel.RIGHT); lX.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel lY = new JLabel("Posisi Y:", JLabel.RIGHT); lY.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel lW = new JLabel("Lebar (W):", JLabel.RIGHT); lW.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel lH = new JLabel("Tinggi (H):", JLabel.RIGHT); lH.setFont(new Font("SansSerif", Font.PLAIN, 11));

        spinGrid.add(lX); spinGrid.add(spX);
        spinGrid.add(lY); spinGrid.add(spY);
        spinGrid.add(lW); spinGrid.add(spWidth);
        spinGrid.add(lH); spinGrid.add(spHeight);

        content.add(spinGrid, BorderLayout.NORTH);

        // Quick Preset Buttons
        JPanel presetBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        presetBar.setOpaque(false);

        JButton btnStd = new JButton("↔ 180×150 px");
        btnStd.setFont(new Font("SansSerif", Font.PLAIN, 10));
        btnStd.setToolTipText("Standarkan ukuran ke 180 × 150 px");
        btnStd.addActionListener(e -> {
            spWidth.setValue(180);
            spHeight.setValue(150);
            updatePreview();
        });

        JButton btnAlignLeft = new JButton("⇤ X = 60");
        btnAlignLeft.setFont(new Font("SansSerif", Font.PLAIN, 10));
        btnAlignLeft.setToolTipText("Ratakan sisi kiri ke margin 60 px");
        btnAlignLeft.addActionListener(e -> {
            spX.setValue(60);
            updatePreview();
        });

        JButton btnNudge = new JButton("+30 px");
        btnNudge.setFont(new Font("SansSerif", Font.PLAIN, 10));
        btnNudge.setToolTipText("Geser posisi diagonal +30 px");
        btnNudge.addActionListener(e -> {
            spX.setValue((Integer) spX.getValue() + 30);
            spY.setValue((Integer) spY.getValue() + 30);
            updatePreview();
        });

        presetBar.add(btnStd);
        presetBar.add(btnAlignLeft);
        presetBar.add(btnNudge);

        content.add(presetBar, BorderLayout.CENTER);

        // Interactive Mini Canvas Preview
        previewRect = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Dot-grid background
                g2.setColor(new Color(241, 245, 249));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(226, 232, 240));
                for (int x = 12; x < w; x += 14) {
                    for (int y = 12; y < h; y += 14) {
                        g2.fillRect(x, y, 1, 1);
                    }
                }

                // Scaled Room Box
                int curW = (spWidth != null) ? ((Number) spWidth.getValue()).intValue() : room.getWidth();
                int curH = (spHeight != null) ? ((Number) spHeight.getValue()).intValue() : room.getHeight();

                int rw = Math.min(w - 24, Math.max(50, curW / 2));
                int rh = Math.min(h - 24, Math.max(28, curH / 2));
                int rx = (w - rw) / 2;
                int ry = (h - rh) / 2;

                boolean isAvail = room.isAvailable();
                Color boxBg = isAvail ? new Color(16, 185, 129, 210) : new Color(239, 68, 68, 210);
                Color boxBorder = isAvail ? new Color(5, 150, 105) : new Color(185, 28, 28);

                g2.setColor(boxBg);
                g2.fillRoundRect(rx, ry, rw, rh, 8, 8);
                g2.setColor(boxBorder);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(rx, ry, rw, rh, 8, 8);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                String label = (txtCode != null && !txtCode.getText().trim().isEmpty()) ? txtCode.getText().trim() : room.getCode();
                int strW = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, rx + Math.max(4, (rw - strW) / 2), ry + (rh / 2) + 4);

                g2.setColor(new Color(71, 85, 105));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                String dimStr = curW + " × " + curH + " px";
                g2.drawString(dimStr, 8, h - 6);

                g2.dispose();
            }
        };
        previewRect.setPreferredSize(new Dimension(200, 85));
        previewRect.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        content.add(previewRect, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCardContainer(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTitle.setForeground(new Color(100, 116, 139)); // Slate 500
        card.add(lblTitle, BorderLayout.NORTH);

        return card;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(241, 245, 249));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                new EmptyBorder(10, 20, 10, 20)
        ));

        // Left Actions: Delete & Duplicate
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);

        JButton btnDelete = new JButton("🗑 Hapus Ruangan");
        btnDelete.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnDelete.setForeground(new Color(185, 28, 28)); // Red 700
        btnDelete.setBackground(new Color(254, 242, 242)); // Red 50
        btnDelete.addActionListener(e -> handleDelete());

        JButton btnDup = new JButton("⧉ Duplikasi");
        btnDup.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnDup.addActionListener(e -> handleDuplicate());

        leftActions.add(btnDelete);
        leftActions.add(btnDup);

        // Right Actions: Cancel & Save
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        JButton btnCancel = new JButton("Batal");
        btnCancel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton("💾 Simpan Perubahan");
        btnSave.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnSave.setBackground(new Color(16, 185, 129)); // Emerald Green
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> handleSave());

        rightActions.add(btnCancel);
        rightActions.add(btnSave);

        footer.add(leftActions, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);
        return footer;
    }

    private void updatePreview() {
        if (previewRect != null) {
            previewRect.repaint();
        }
    }

    private void handleSave() {
        try {
            spCapacity.commitEdit();
            spX.commitEdit();
            spY.commitEdit();
            spWidth.commitEdit();
            spHeight.commitEdit();
        } catch (java.text.ParseException pe) {
            JOptionPane.showMessageDialog(this, "Format nilai angka koordinat atau kapasitas tidak valid!", "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String code = txtCode.getText().trim();
        String name = txtName.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kode dan Nama Ruangan tidak boleh kosong!", "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cap = ((Number) spCapacity.getValue()).intValue();
        if (cap <= 0) {
            JOptionPane.showMessageDialog(this, "Kapasitas ruangan harus lebih besar dari 0!", "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate uniqueness of room code within building
        if (building != null) {
            List<Room> existing = roomDao.getRoomsByBuildingId(building.getId());
            for (Room r : existing) {
                if (r.getId() != room.getId() && r.getCode().equalsIgnoreCase(code)) {
                    JOptionPane.showMessageDialog(this,
                            "Kode ruangan '" + code + "' sudah digunakan oleh ruangan lain di gedung ini!\nSilakan gunakan kode ruangan yang unik.",
                            "Validasi Gagal", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        // Prepare updated room object without mutating original until DB succeeds
        Room updated = new Room(
                room.getId(),
                room.getBuildingId(),
                code,
                name,
                room.getFloor(),
                cap,
                (String) cbType.getSelectedItem(),
                txtFacilities.getText().trim(),
                ((Number) spX.getValue()).intValue(),
                ((Number) spY.getValue()).intValue(),
                ((Number) spWidth.getValue()).intValue(),
                ((Number) spHeight.getValue()).intValue()
        );
        updated.setAvailable(room.isAvailable());
        updated.setCurrentOccupantInfo(room.getCurrentOccupantInfo());

        // Update database
        boolean ok = roomDao.updateRoom(updated);
        if (ok) {
            // Apply changes to in-memory room object safely
            room.setCode(updated.getCode());
            room.setName(updated.getName());
            room.setRoomType(updated.getRoomType());
            room.setCapacity(updated.getCapacity());
            room.setFacilities(updated.getFacilities());
            room.setPosX(updated.getPosX());
            room.setPosY(updated.getPosY());
            room.setWidth(updated.getWidth());
            room.setHeight(updated.getHeight());

            dispose();
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            JOptionPane.showMessageDialog(getParent(),
                    "✅ Properti & koordinat ruangan " + code + " berhasil disimpan!",
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan perubahan ke database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Apakah Anda yakin ingin menghapus ruangan [" + room.getCode() + " - " + room.getName() + "]?",
                "Konfirmasi Hapus Ruangan",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (roomDao.deleteRoom(room.getId())) {
                dispose();
                if (onDeleteCallback != null) {
                    onDeleteCallback.run();
                }
                JOptionPane.showMessageDialog(getParent(), "Ruangan berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Gagal menghapus ruangan.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDuplicate() {
        dispose();
        if (onDuplicateCallback != null) {
            onDuplicateCallback.run();
        }
    }
}
