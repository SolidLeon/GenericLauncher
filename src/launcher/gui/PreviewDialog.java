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
import launcher.beans.UpdateBean;
import launcher.beans.xml.XmlComponentBean;

public class PreviewDialog extends JDialog {

	public enum PreviewResult {
		OK,
		CANCEL
	};
	
	private JTable table;
	private List<UpdateBean> componentBeans;
	private JButton okButton;
	private JButton cancelButton;
	private PreviewResult previewResult = null;
	
	public PreviewDialog(JFrame owner, List<UpdateBean> componentBeans) {
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
			
			for (int i = 0; i < componentBeans.size(); i++) {
				UpdateBean componentBean = componentBeans.get(i);
				if (!componentBean.download) componentBeans.remove(i--);
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
	
	
	class PreviewTableModel extends DefaultTableModel {

		@Override
		public boolean isCellEditable(int row, int column) {

			// column 0 is "download" so it should be only editable if the
			// required flag for the component at "row" is false
			if (column == 0) {
				UpdateBean component = componentBeans.get(row);
				return !component.remote.required;
			}
			
			return false;
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0: 
				return Boolean.class;
			case 1:
				return String.class;
			case 2: 
				return String.class;
			case 3:
				return String.class;
			case 4:
				return String.class;
			default:
				return null;
			}
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0: return "";
			case 1: return "File";
			case 2: return "Remote Version";
			case 3: return "Local Version";
			case 4: return "Local Compare File";
			default:
				return null;
			}
		}
		
		@Override
		public int getColumnCount() {
			return 5;
		}
		
		@Override
		public int getRowCount() {
			return componentBeans.size();
		}
		
		@Override
		public Object getValueAt(int row, int column) {
			UpdateBean bean = componentBeans.get(row);
			switch (column) {
			case 0:
				return bean.download;
			case 1:
				return bean.remote.source;
			case 2: 
				return bean.remote.version;
			case 3:
				return bean.local == null ? "-" : bean.local.version;
			case 4:
				return bean.remote.compare == null ? "" : bean.remote.compare;
			default:
				return null;
			}
		}
		
		@Override
		public void setValueAt(Object aValue, int row, int column) {
			// "Download" column
			if (column == 0) {
				boolean newValue = (boolean) aValue;
				UpdateBean componentBean = componentBeans.get(row);
				componentBean.download = newValue;
				fireTableRowsUpdated(row, row);
			} else {
				super.setValueAt(aValue, row, column);
			}
		}
	}
}
