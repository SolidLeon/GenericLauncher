package launcher.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import launcher.beans.ComponentBean;

public class PreviewDialog extends JDialog {

	public enum PreviewResult {
		OK,
		CANCEL
	};
	
	private JTable table;
	private List<ComponentBean> componentBeans;
	private JButton okButton;
	private JButton cancelButton;
	private PreviewResult previewResult = null;
	
	public PreviewDialog(JFrame owner, List<ComponentBean> componentBeans) {
		super(owner, true);
		this.componentBeans = componentBeans;
//		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		table = new JTable(new PreviewTableModel());
		add(new JScrollPane(table));
		JPanel buttonPanel = new JPanel(new FlowLayout());
		cancelButton = new JButton("Cancel");
		okButton = new JButton("OK");
		buttonPanel.add(cancelButton);
		buttonPanel.add(okButton);
		add(buttonPanel, BorderLayout.SOUTH);
		pack();
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (previewResult == null) {
					previewResult = PreviewResult.CANCEL;
				}
			}
		});

		okButton.addActionListener(evt -> {
			previewResult = PreviewResult.OK;
			setVisible(false);
		});
		cancelButton.addActionListener(evt -> {
			previewResult = PreviewResult.CANCEL;
			setVisible(false);
		});
		
		setLocationRelativeTo(owner);
	}
	
	public PreviewResult getPreviewResult() {
		return previewResult;
	}
	
	class PreviewTableModel extends DefaultTableModel {

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public String getColumnName(int column) {
			return column == 0 ? "Source" : column == 1 ? "Target" : null;
		}
		
		@Override
		public int getColumnCount() {
			return 2;
		}
		
		@Override
		public int getRowCount() {
			return componentBeans.size();
		}
		
		@Override
		public Object getValueAt(int row, int column) {
			ComponentBean bean = componentBeans.get(row);
			return column == 0 ? bean.getSource().getAbsolutePath() : column == 1 ? bean.getTarget().getAbsolutePath() : null;
		}
	}
}
