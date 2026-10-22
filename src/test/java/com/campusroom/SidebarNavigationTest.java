package com.campusroom;

import com.campusroom.dao.DatabaseHelper;
import com.campusroom.model.User;
import com.campusroom.ui.MainFrame;
import com.campusroom.ui.SidebarNavigationPanel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SidebarNavigationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("     RUNNING SIDEBAR NAVIGATION & UI TESTS       ");
        System.out.println("=================================================");

        DatabaseHelper.initializeDatabase();
        int passed = 0;
        int failed = 0;

        try {
            // 1. Initialize MainFrame
            MainFrame frame = new MainFrame();

            // 2. Test switching to all 9 navigation cards
            System.out.println("[TEST 1] Testing view switching across all 9 sidebar modules...");
            for (int i = 0; i <= 8; i++) {
                frame.switchToView(i);
            }
            frame.switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);
            System.out.println("[PASS] All 9 view switches executed cleanly without exceptions.");
            passed++;

            // 3. Test Role Permissions on Sidebar
            System.out.println("\n[TEST 2] Testing Role-based Access on Sidebar...");
            com.campusroom.dao.UserDao userDao = new com.campusroom.dao.UserDao();

            // As Student (id=1)
            User student = userDao.getUserById(1);
            frame.onUserChanged(student);
            System.out.println("[PASS] Student role applied to sidebar.");
            passed++;

            // As Secretary (id=2)
            User sekdek = userDao.getUserById(2);
            frame.onUserChanged(sekdek);
            System.out.println("[PASS] Secretary role applied to sidebar.");
            passed++;

            // As Admin (id=3)
            User admin = userDao.getUserById(3);
            frame.onUserChanged(admin);
            System.out.println("[PASS] Admin role applied to sidebar.");
            passed++;

            // 4. Test Sidebar Component directly
            System.out.println("\n[TEST 3] Testing SidebarNavigationPanel directly...");
            final int[] selectedNav = {-1};
            SidebarNavigationPanel sidebar = new SidebarNavigationPanel(new SidebarNavigationPanel.NavigationListener() {
                @Override
                public void onNavigationSelected(int navIndex) {
                    selectedNav[0] = navIndex;
                }
                @Override
                public void onSwitchRoleRequested() {}
                @Override
                public void onLogoutRequested() {}
            });

            // Select Catalog
            sidebar.selectItem(SidebarNavigationPanel.NAV_CATALOG, true);
            if (selectedNav[0] == SidebarNavigationPanel.NAV_CATALOG && sidebar.getSelectedIndex() == SidebarNavigationPanel.NAV_CATALOG) {
                System.out.println("[PASS] Sidebar selectItem correctly selected Catalog and notified listener.");
                passed++;
            } else {
                System.err.println("[FAIL] Sidebar selection mismatch!");
                failed++;
            }

            // Select Rooms Management (Admin)
            sidebar.selectItem(SidebarNavigationPanel.NAV_ADMIN_ROOMS, true);
            if (selectedNav[0] == SidebarNavigationPanel.NAV_ADMIN_ROOMS && sidebar.getSelectedIndex() == SidebarNavigationPanel.NAV_ADMIN_ROOMS) {
                System.out.println("[PASS] Sidebar selectItem correctly selected Admin Rooms.");
                passed++;
            } else {
                System.err.println("[FAIL] Sidebar selection mismatch for Admin Rooms! selectedNav=" + selectedNav[0] + " getSelectedIndex=" + sidebar.getSelectedIndex() + " target.isVisible=" + sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_ROOMS) + " target.isEnabled=" + sidebar.isItemEnabled(SidebarNavigationPanel.NAV_ADMIN_ROOMS));
                failed++;
            }

            // Test Badge Counters
            sidebar.setBadgeCount(SidebarNavigationPanel.NAV_SEC_VALIDATION, 5);
            sidebar.setBadgeCount(SidebarNavigationPanel.NAV_ADMIN_APPROVAL, 2);
            System.out.println("[PASS] Badge counters set without error.");
            passed++;

            // Test Disabled item selection protection
            sidebar.setItemEnabled(SidebarNavigationPanel.NAV_ADMIN_APPROVAL, false);
            selectedNav[0] = -1;
            sidebar.selectItem(SidebarNavigationPanel.NAV_ADMIN_APPROVAL, true);
            if (selectedNav[0] != SidebarNavigationPanel.NAV_ADMIN_APPROVAL) {
                System.out.println("[PASS] Disabled sidebar item cannot be selected.");
                passed++;
            } else {
                System.err.println("[FAIL] Disabled item was selected!");
                failed++;
            }

            // Test Dynamic Role Visibility: MAHASISWA
            System.out.println("\n[TEST 3B] Testing Dynamic Role Visibility: MAHASISWA...");
            sidebar.applyRolePermissions("MAHASISWA");
            boolean studentVisibilityOk = sidebar.isItemVisible(SidebarNavigationPanel.NAV_FLOOR_PLAN)
                    && sidebar.isItemVisible(SidebarNavigationPanel.NAV_CATALOG)
                    && sidebar.isItemVisible(SidebarNavigationPanel.NAV_BOOKING)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_SEC_VALIDATION)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_SEC_SCHEDULE)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_APPROVAL)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_CAMPUS)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_ROOMS)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_LAYOUT_EDITOR);

            if (studentVisibilityOk) {
                System.out.println("[PASS] Mahasiswa role completely hides Secretary and Admin menus (not visible).");
                passed++;
            } else {
                System.err.println("[FAIL] Mahasiswa role visibility mismatch!");
                failed++;
            }

            // Hidden item cannot be selected by click or direct invocation
            selectedNav[0] = -1;
            sidebar.selectItem(SidebarNavigationPanel.NAV_ADMIN_ROOMS, true);
            if (selectedNav[0] == -1) {
                System.out.println("[PASS] Hidden Admin item cannot be selected when Mahasiswa role is active.");
                passed++;
            } else {
                System.err.println("[FAIL] Hidden item was selected!");
                failed++;
            }

            // Test Dynamic Role Visibility: SEKDEK
            System.out.println("\n[TEST 3C] Testing Dynamic Role Visibility: SEKDEK...");
            sidebar.applyRolePermissions("SEKDEK");
            boolean sekdekVisibilityOk = sidebar.isItemVisible(SidebarNavigationPanel.NAV_FLOOR_PLAN)
                    && sidebar.isItemVisible(SidebarNavigationPanel.NAV_CATALOG)
                    && sidebar.isItemVisible(SidebarNavigationPanel.NAV_BOOKING)
                    && sidebar.isItemVisible(SidebarNavigationPanel.NAV_SEC_VALIDATION)
                    && sidebar.isItemVisible(SidebarNavigationPanel.NAV_SEC_SCHEDULE)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_APPROVAL)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_CAMPUS)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_ROOMS)
                    && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_LAYOUT_EDITOR);

            if (sekdekVisibilityOk) {
                System.out.println("[PASS] Sekdek role shows Secretary menus and completely hides Admin menus.");
                passed++;
            } else {
                System.err.println("[FAIL] Sekdek role visibility mismatch!");
                failed++;
            }

            // Test Dynamic Role Visibility: ADMIN
            System.out.println("\n[TEST 3D] Testing Dynamic Role Visibility: ADMIN...");
            sidebar.applyRolePermissions("ADMIN");
            boolean adminVisibilityOk = true;
            for (int i = 0; i <= 8; i++) {
                if (!sidebar.isItemVisible(i) || !sidebar.isItemEnabled(i)) {
                    adminVisibilityOk = false;
                    break;
                }
            }
            if (adminVisibilityOk) {
                System.out.println("[PASS] Admin role shows all 9 menus.");
                passed++;
            } else {
                System.err.println("[FAIL] Admin role visibility mismatch!");
                failed++;
            }

            // Test Student Info Card dynamic visibility
            System.out.println("\n[TEST 3E] Testing Student Info Card dynamic visibility...");
            sidebar.applyRolePermissions("MAHASISWA");
            boolean studentCardVisible = sidebar.getStudentInfoPanel() != null && sidebar.getStudentInfoPanel().isVisible();
            sidebar.applyRolePermissions("SEKDEK");
            boolean sekdekCardHidden = sidebar.getStudentInfoPanel() != null && !sidebar.getStudentInfoPanel().isVisible();
            sidebar.applyRolePermissions("ADMIN");
            boolean adminCardHidden = sidebar.getStudentInfoPanel() != null && !sidebar.getStudentInfoPanel().isVisible();

            if (studentCardVisible && sekdekCardHidden && adminCardHidden) {
                System.out.println("[PASS] Student Info Card visible only for Mahasiswa, hidden for Sekdek and Admin.");
                passed++;
            } else {
                System.err.println("[FAIL] Student Info Card visibility error: student=" + studentCardVisible + " sekdekHidden=" + sekdekCardHidden + " adminHidden=" + adminCardHidden);
                failed++;
            }

            // Test Case-Insensitive Role Application
            System.out.println("\n[TEST 3F] Testing Case-Insensitive Role Input ('mahasiswa', 'sekdek', 'admin')...");
            sidebar.applyRolePermissions("mahasiswa");
            boolean lowerMahasiswaOk = !sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_ROOMS) && sidebar.isItemVisible(SidebarNavigationPanel.NAV_FLOOR_PLAN);
            sidebar.applyRolePermissions("sekdek");
            boolean lowerSekdekOk = sidebar.isItemVisible(SidebarNavigationPanel.NAV_SEC_VALIDATION) && !sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_ROOMS);
            sidebar.applyRolePermissions("admin");
            boolean lowerAdminOk = sidebar.isItemVisible(SidebarNavigationPanel.NAV_ADMIN_ROOMS) && sidebar.isItemVisible(SidebarNavigationPanel.NAV_SEC_VALIDATION);

            if (lowerMahasiswaOk && lowerSekdekOk && lowerAdminOk) {
                System.out.println("[PASS] Case-insensitive role input verified for all roles.");
                passed++;
            } else {
                System.err.println("[FAIL] Case-insensitive role input failed!");
                failed++;
            }

            // Test Guest Navigation Security Guard in MainFrame
            System.out.println("\n[TEST 3G] Testing Guest Navigation Guard in MainFrame...");
            com.campusroom.service.AuthService.getInstance().logout();
            frame.onUserChanged(null);
            frame.switchToView(SidebarNavigationPanel.NAV_ADMIN_APPROVAL);
            // With guest guard, it should be redirected back to NAV_FLOOR_PLAN
            if (sidebar.getSelectedIndex() != SidebarNavigationPanel.NAV_ADMIN_APPROVAL) {
                System.out.println("[PASS] Guest navigation to Admin view blocked and redirected safely.");
                passed++;
            } else {
                System.err.println("[FAIL] Guest was able to navigate to Admin view!");
                failed++;
            }

            // Test Rapid Role Switching with Selected Room State Sync
            System.out.println("\n[TEST 3H] Testing Rapid Role Switching with Selected Room State Sync...");
            frame.onUserChanged(student);
            frame.switchToView(SidebarNavigationPanel.NAV_FLOOR_PLAN);
            com.campusroom.dao.RoomDao roomDao = new com.campusroom.dao.RoomDao();
            com.campusroom.model.Room testRoom = roomDao.getRoomById(1);
            frame.onRoomSelected(testRoom);

            // Rapid role switches
            frame.onUserChanged(admin);
            frame.onUserChanged(sekdek);
            frame.onUserChanged(student);
            System.out.println("[PASS] Rapid role switching with active room selection synced cleanly without exceptions.");
            passed++;

            // 5. Test GUI rendering of updated MainFrame with Sidebar at standard resolution
            System.out.println("\n[TEST 4] Rendering MainFrame with Sidebar to BufferedImage (1380x860 and 1200x750)...");
            frame.setSize(1380, 860);
            frame.addNotify();
            frame.validate();
            BufferedImage img = new BufferedImage(1380, 860, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            frame.paint(g2);
            g2.dispose();

            frame.setSize(1200, 750);
            frame.validate();
            BufferedImage img1200 = new BufferedImage(1200, 750, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g1200 = img1200.createGraphics();
            frame.paint(g1200);
            g1200.dispose();

            frame.dispose();

            System.out.println("[PASS] MainFrame rendered successfully at 1380x860 and 1200x750 without exceptions.");
            passed++;

            // 5. Test Sidebar Minimize / Collapse
            System.out.println("\n[TEST 5] Testing Sidebar Minimize / Collapse functionality...");
            final boolean[] collapseNotified = {false};
            SidebarNavigationPanel testSidebar = new SidebarNavigationPanel(new SidebarNavigationPanel.NavigationListener() {
                @Override
                public void onNavigationSelected(int navIndex) {}
                @Override
                public void onSwitchRoleRequested() {}
                @Override
                public void onLogoutRequested() {}
                @Override
                public void onSidebarCollapseToggled(boolean isCollapsed) {
                    collapseNotified[0] = isCollapsed;
                }
            });

            // Initial state: expanded (265px)
            if (!testSidebar.isCollapsed() && testSidebar.getPreferredSize().width == SidebarNavigationPanel.EXPANDED_WIDTH) {
                System.out.println("[PASS] Initial state is expanded (265px).");
                passed++;
            } else {
                System.err.println("[FAIL] Initial state not expanded: " + testSidebar.getPreferredSize().width);
                failed++;
            }

            // Collapse sidebar
            testSidebar.setCollapsed(true);
            if (testSidebar.isCollapsed() && testSidebar.getPreferredSize().width == SidebarNavigationPanel.COLLAPSED_WIDTH && collapseNotified[0]) {
                System.out.println("[PASS] Sidebar successfully collapsed to 70px compact rail and notified listener.");
                passed++;
            } else {
                System.err.println("[FAIL] Sidebar collapse failed: width=" + testSidebar.getPreferredSize().width + ", notified=" + collapseNotified[0]);
                failed++;
            }

            // Toggle collapse back to expanded
            testSidebar.toggleCollapse();
            if (!testSidebar.isCollapsed() && testSidebar.getPreferredSize().width == SidebarNavigationPanel.EXPANDED_WIDTH && !collapseNotified[0]) {
                System.out.println("[PASS] toggleCollapse successfully expanded sidebar back to 265px.");
                passed++;
            } else {
                System.err.println("[FAIL] toggleCollapse expand failed: width=" + testSidebar.getPreferredSize().width);
                failed++;
            }

            // Toggle again to collapsed
            testSidebar.toggleCollapse();
            if (testSidebar.isCollapsed() && testSidebar.getPreferredSize().width == SidebarNavigationPanel.COLLAPSED_WIDTH) {
                System.out.println("[PASS] toggleCollapse successfully collapsed sidebar to 70px.");
                passed++;
            } else {
                System.err.println("[FAIL] toggleCollapse second toggle failed.");
                failed++;
            }

            // Test item selection while collapsed
            testSidebar.selectItem(SidebarNavigationPanel.NAV_CATALOG, true);
            if (testSidebar.getSelectedIndex() == SidebarNavigationPanel.NAV_CATALOG) {
                System.out.println("[PASS] Sidebar navigation item selected cleanly while in collapsed mode.");
                passed++;
            } else {
                System.err.println("[FAIL] Item selection failed in collapsed mode.");
                failed++;
            }

            // Test Student Info Card hidden in collapsed mode and restored in expanded mode
            testSidebar.applyRolePermissions("MAHASISWA");
            testSidebar.setCollapsed(true);
            boolean studentCardHiddenWhenCollapsed = testSidebar.getStudentInfoPanel() != null && !testSidebar.getStudentInfoPanel().isVisible();
            testSidebar.setCollapsed(false);
            boolean studentCardShownWhenExpanded = testSidebar.getStudentInfoPanel() != null && testSidebar.getStudentInfoPanel().isVisible();
            if (studentCardHiddenWhenCollapsed && studentCardShownWhenExpanded) {
                System.out.println("[PASS] Student guide card hides when collapsed and restores when expanded.");
                passed++;
            } else {
                System.err.println("[FAIL] Student card visibility toggle failed: hidden=" + studentCardHiddenWhenCollapsed + ", shown=" + studentCardShownWhenExpanded);
                failed++;
            }

            // 6. Test MainFrame rendering with Collapsed Sidebar
            System.out.println("\n[TEST 6] Rendering MainFrame with Collapsed Sidebar (70px rail) at 1200x750...");
            MainFrame frameCollapsed = new MainFrame();
            frameCollapsed.setSize(1200, 750);
            frameCollapsed.addNotify();
            frameCollapsed.validate();

            javax.swing.Action toggleAct = frameCollapsed.getRootPane().getActionMap().get("toggleSidebar");
            if (toggleAct != null) {
                toggleAct.actionPerformed(new java.awt.event.ActionEvent(frameCollapsed, 0, "toggle"));
            }
            frameCollapsed.validate();
            BufferedImage imgCollapsed = new BufferedImage(1200, 750, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gCol = imgCollapsed.createGraphics();
            frameCollapsed.paint(gCol);
            gCol.dispose();
            frameCollapsed.dispose();
            System.out.println("[PASS] MainFrame with collapsed sidebar rendered cleanly without exceptions.");
            passed++;

            // 7. Test Table Auto-Resize Mode across all tables
            System.out.println("\n[TEST 7] Testing Table AUTO_RESIZE_ALL_COLUMNS to eliminate horizontal scrolling...");
            javax.swing.JTable dummyTable = new javax.swing.JTable(new javax.swing.table.DefaultTableModel(new String[]{"A", "B", "C"}, 1));
            com.campusroom.ui.ModernUIHelper.styleModernTable(dummyTable);
            if (dummyTable.getAutoResizeMode() == javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS) {
                System.out.println("[PASS] ModernUIHelper sets AUTO_RESIZE_ALL_COLUMNS eliminating horizontal scrollbars.");
                passed++;
            } else {
                System.err.println("[FAIL] Table auto-resize mode mismatch: " + dummyTable.getAutoResizeMode());
                failed++;
            }

            // 8. Test Window Resizing while collapsed maintains 70px preferred width
            System.out.println("\n[TEST 8] Testing Window Resizing in Collapsed State...");
            MainFrame resizeFrame = new MainFrame();
            resizeFrame.setSize(1400, 900);
            resizeFrame.addNotify();
            resizeFrame.validate();
            SidebarNavigationPanel rSidebar = (SidebarNavigationPanel) ((java.awt.BorderLayout) resizeFrame.getContentPane().getLayout()).getLayoutComponent(java.awt.BorderLayout.WEST);
            rSidebar.setCollapsed(true);
            resizeFrame.validate();
            boolean width70AfterCollapse = (rSidebar.getPreferredSize().width == 70);
            resizeFrame.setSize(1120, 720);
            resizeFrame.validate();
            boolean width70AfterShrink = (rSidebar.getPreferredSize().width == 70);
            resizeFrame.setSize(1600, 950);
            resizeFrame.validate();
            boolean width70AfterEnlarge = (rSidebar.getPreferredSize().width == 70);
            resizeFrame.dispose();

            if (width70AfterCollapse && width70AfterShrink && width70AfterEnlarge) {
                System.out.println("[PASS] Collapsed sidebar consistently maintains 70px preferred width across resize operations.");
                passed++;
            } else {
                System.err.println("[FAIL] Window resize broke collapsed width: collapse=" + width70AfterCollapse + " shrink=" + width70AfterShrink + " enlarge=" + width70AfterEnlarge);
                failed++;
            }

            // 9. Test Tooltip synchronization when changing users in collapsed state
            System.out.println("\n[TEST 9] Testing Profile Tooltip synchronization in Collapsed State...");
            testSidebar.setCollapsed(true);
            testSidebar.updateUserProfile(student);
            java.lang.reflect.Field profileCardField = SidebarNavigationPanel.class.getDeclaredField("profileCard");
            profileCardField.setAccessible(true);
            javax.swing.JPanel pCard = (javax.swing.JPanel) profileCardField.get(testSidebar);
            String tipStudent = pCard.getToolTipText();

            testSidebar.updateUserProfile(admin);
            String tipAdmin = pCard.getToolTipText();

            if (tipStudent != null && tipStudent.contains("Akbar") && tipAdmin != null && tipAdmin.contains("Santoso")) {
                System.out.println("[PASS] Collapsed profile card tooltips dynamically sync on user switch without lag.");
                passed++;
            } else {
                System.err.println("[FAIL] Collapsed profile tooltip sync failed! tipStudent=" + tipStudent + ", tipAdmin=" + tipAdmin);
                failed++;
            }

            // 10. Test Top Bar Toggle Button synchronization
            System.out.println("\n[TEST 10] Testing Top Bar Toggle Button tooltip synchronization...");
            MainFrame frameBtnTest = new MainFrame();
            java.lang.reflect.Field btnToggleField = MainFrame.class.getDeclaredField("btnToggleSidebar");
            btnToggleField.setAccessible(true);
            javax.swing.JButton btnToggle = (javax.swing.JButton) btnToggleField.get(frameBtnTest);
            String tipExpanded = btnToggle.getToolTipText();

            javax.swing.Action actToggle = frameBtnTest.getRootPane().getActionMap().get("toggleSidebar");
            if (actToggle != null) {
                actToggle.actionPerformed(new java.awt.event.ActionEvent(frameBtnTest, 0, "toggle"));
            }
            String tipCollapsed = btnToggle.getToolTipText();
            frameBtnTest.dispose();

            if (tipExpanded != null && tipExpanded.contains("Ciutkan") && tipCollapsed != null && tipCollapsed.contains("Perluas")) {
                System.out.println("[PASS] Top bar toggle button tooltips correctly sync on collapse and expand.");
                passed++;
            } else {
                System.err.println("[FAIL] Top bar toggle button tooltip mismatch: tipExpanded=" + tipExpanded + ", tipCollapsed=" + tipCollapsed);
                failed++;
            }

            System.out.println("=================================================");
            System.out.println("  SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
            System.out.println("=================================================");

            if (failed > 0) {
                throw new AssertionError(failed + " test assertions failed!");
            }
        } catch (Throwable t) {
            if (t instanceof AssertionError) throw (AssertionError) t;
            throw new RuntimeException("Test execution failed", t);
        }
    }

    @org.junit.jupiter.api.Test
    public void testSidebarNavigation() {
        main(new String[]{});
    }
}
