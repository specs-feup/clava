/**
 * Copyright 2015 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.suikasoft.jOptions.Interfaces.DataStore;

import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.syntaxkits.CppSyntaxKit;
import pt.up.fe.specs.util.SpecsIo;

public class ClavaViewerFrame extends JFrame {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final DataStore data;

    private final JTabbedPane codeTab;
    private final JEditorPane cppCodeView;
    private final JScrollPane cppCodeScroll;
    private final JEditorPane astView;
    private final JScrollPane astScroll;
    private final JTextPane diagnosticsView;
    private final JScrollPane diagnosticsScroll;
    private final JCheckBoxMenuItem useCpp11;

    private final Style commonStyle, errorStyle;

    public ClavaViewerFrame() {
        super("Clava AST");

        data = DataStore.newInstance("ClavaPrinter Data");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DefaultSyntaxKit.initKit();
        DefaultSyntaxKit.registerContentType("text/cpp", CppSyntaxKit.class.getName());
        // DefaultSyntaxKit.registerContentType("text/lara", LaraSyntaxKit.class.getName());
        // DefaultSyntaxKit.registerContentType("application/x-matisse-bytecode", BytecodeSyntaxKit.class.getName());
        // DefaultSyntaxKit.registerContentType("application/x-matisse-ast", AstSyntaxKit.class.getName());

        cppCodeView = new JEditorPane();
        cppCodeScroll = new JScrollPane(cppCodeView);
        cppCodeView.setContentType("text/cpp");
        cppCodeView.setText(SpecsIo.getResource(ClavaViewerResource.TEST));

        codeTab = new JTabbedPane();
        codeTab.addTab("C/C++", cppCodeScroll);

        astView = new JEditorPane();
        astScroll = new JScrollPane(astView);
        astView.setContentType("text/cpp");

        diagnosticsView = new JTextPane();
        diagnosticsScroll = new JScrollPane(diagnosticsView);

        commonStyle = diagnosticsView.addStyle("common", null);
        StyleConstants.setForeground(commonStyle, Color.BLACK);
        errorStyle = diagnosticsView.addStyle("error", null);
        StyleConstants.setForeground(errorStyle, Color.RED);

        diagnosticsView.setEditable(false);
        diagnosticsView.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(evt -> {
            cppCodeView.setText("");
            astView.setText("");
        });
        fileMenu.add(newItem);
        JMenuItem openItem = new JMenuItem("Open File...");
        openItem.addActionListener(evt -> {
            JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showOpenDialog(ClavaViewerFrame.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {

                cppCodeView.setText(SpecsIo.read(chooser.getSelectedFile()));
                /*
                try {
                    try (FileReader reader = new FileReader(chooser.getSelectedFile())) {
                        cppCodeView.setText(readAll(reader));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
            }
        });
        fileMenu.add(openItem);
        menuBar.add(fileMenu);

        JMenu parseMenu = new JMenu("Clava");

        useCpp11 = new JCheckBoxMenuItem("C++11");
        useCpp11.setSelected(true);
        useCpp11.addActionListener(evt -> data.set(ClavaViewerKeys.IS_CPP, useCpp11.isSelected()));
        parseMenu.add(useCpp11);

        menuBar.add(parseMenu);

        setJMenuBar(menuBar);

        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel();

        DemoMode[] values = DemoMode.values();
        for (int i = 0; i < values.length; i++) {
            DemoMode mode = values[i];
            JButton button = new JButton(mode.getLabel());
            button.addActionListener(evt -> {
                // applyPassesItem.setSelected(mode.getApplyPasses());
                // convertToCssaItem.setSelected(mode.getConvertToCssa());

                parseCode(mode.getPrinter(), data);
                // int menuIndex = Arrays.asList(PrintMode.values()).indexOf(mode.getMode());
                // modeItems.get(menuIndex).doClick();
            });
            toolbar.add(button);
        }

        add(toolbar, BorderLayout.NORTH);
        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, codeTab, astScroll);
        add(horizontalSplit, BorderLayout.CENTER);
        horizontalSplit.setDividerLocation(400);
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, horizontalSplit, diagnosticsScroll);
        verticalSplit.setDividerLocation(500);
        add(verticalSplit);

        setSize(1200, 700);

        // modeItems.get(1).doClick();
    }

    private void parseCode(CodeViewer printer, DataStore data)

    {

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            PrintStream reportStream = new PrintStream(byteStream);

            astView.setContentType(printer.getContentType());

            diagnosticsView.setText("");
            diagnosticsScroll.updateUI();

            try {
                addDiagnosticsText("Starting...\n", commonStyle);
                long start = System.nanoTime();

                String code = cppCodeView.getText();

                String result = printer.getCode(code, data);

                long end = System.nanoTime();
                addDiagnosticsText("Finished in " + Math.round((end - start) * 1.0e-9 * 100) / 100.0 + " seconds.\n",
                        commonStyle);

                astView.setText(result);
                astScroll.updateUI();

            } catch (RuntimeException e) {
                e.printStackTrace(reportStream);
            } finally {
                reportStream.flush();
                String text = byteStream.toString();

                addDiagnosticsText(text, errorStyle);
            }
        } catch (Exception e) {
            e.printStackTrace();

            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
        }

    }

    private void addDiagnosticsText(String text, Style style) throws BadLocationException {
        StyledDocument document = diagnosticsView.getStyledDocument();
        document.insertString(document.getLength(), text, style);
        diagnosticsScroll.updateUI();
    }

}
