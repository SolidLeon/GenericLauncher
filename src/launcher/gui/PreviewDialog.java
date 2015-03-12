package launcher.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import sun.util.logging.resources.logging_fr;
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
		table.setDefaultRenderer(File.class, new FileCellRenderer());
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
			
			for (int i = 0; i < componentBeans.size(); i++) {
				ComponentBean componentBean = componentBeans.get(i);
				if (!componentBean.isDownload()) componentBeans.remove(i--);
			}
			
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
	
	class FileCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
					row, column);
			
			if (value != null && value instanceof File) {
				File f = (File) value;
				if (!f.exists())
					lbl.setForeground(Color.red.darker());
				else
					lbl.setForeground(Color.green.darker());
				lbl.setText(f.getName());
				lbl.setToolTipText(f.getAbsolutePath());
			}
			return lbl;
		}
	}
	
	class PreviewTableModel extends DefaultTableModel {

		@Override
		public boolean isCellEditable(int row, int column) {

			// column 0 is "download" so it should be only editable if the
			// required flag for the component at "row" is false
			if (column == 0) {
				ComponentBean component = componentBeans.get(row);
				return !component.isRequired();
			}
			
			return false;
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0: 
				return Boolean.class;
			case 1:
			case 2:
			case 3: 
				return File.class;
			default:
				return null;
			}
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0: return "Download";
			case 1: return "Source";
			case 2: return "Target";
			case 3: return "Compare";
			default:
				return null;
			}
		}
		
		@Override
		public int getColumnCount() {
			return 4;
		}
		
		@Override
		public int getRowCount() {
			return componentBeans.size();
		}
		
		@Override
		public Object getValueAt(int row, int column) {
			ComponentBean bean = componentBeans.get(row);
			switch (column) {
			case 0:
				return bean.isDownload();
			case 1:
				return bean.getSource();
			case 2:
				return bean.getTarget();
			case 3:
				return bean.getCompare() == null ? "" : bean.getCompare();
			default:
				return null;
			}
		}
		
		@Override
		public void setValueAt(Object aValue, int row, int column) {
			// "Download" column
			if (column == 0) {
				boolean newValue = (boolean) aValue;
				ComponentBean componentBean = componentBeans.get(row);
				componentBean.setDownload(newValue);
				fireTableRowsUpdated(row, row);
			} else {
				super.setValueAt(aValue, row, column);
			}
		}
	}
}
