/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.ActuatorsTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class ActuatorsPanel extends JPanel implements WizardContainer {
	private final static Logger logger = LoggerFactory.getLogger(ActuatorsPanel.class);
	
	private static final String PREF_DIVIDER_POSITION = "ActuatorsPanel.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;
	
	private final Configuration configuration;

	private JTable table;

	private ActuatorsTableModel tableModel;
	private TableRowSorter<ActuatorsTableModel> tableSorter;
	private JTextField searchTextField;
	private JPanel configurationPanel;
	
	private Preferences prefs = Preferences.userNodeForPackage(ActuatorsPanel.class);
	
	public ActuatorsPanel(Configuration configuration) {
		this.configuration = configuration;

		setLayout(new BorderLayout(0, 0));
		tableModel = new ActuatorsTableModel(configuration);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.EAST);

		JLabel lblSearch = new JLabel("Search");
		panel_1.add(lblSearch);

		searchTextField = new JTextField();
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                search();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                search();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                search();
            }
        });
		panel_1.add(searchTextField);
		searchTextField.setColumns(15);

		table = new AutoSelectTextTable(tableModel);
		tableSorter = new TableRowSorter<ActuatorsTableModel>(tableModel);

		final JSplitPane splitPane = new JSplitPane();
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
		splitPane.addPropertyChangeListener("dividerLocation",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						prefs.putInt(PREF_DIVIDER_POSITION,
								splitPane.getDividerLocation());
					}
				});
		add(splitPane, BorderLayout.CENTER);

		configurationPanel = new JPanel();
		configurationPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
		
		splitPane.setLeftComponent(new JScrollPane(table));
		splitPane.setRightComponent(configurationPanel);
		configurationPanel.setLayout(new BorderLayout(0, 0));
		table.setRowSorter(tableSorter);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) {
							return;
						}

						Actuator actuator = getSelectedActuator();

						configurationPanel.removeAll();
						if (actuator != null) {
							Wizard wizard = actuator.getConfigurationWizard();
							if (wizard != null) {
								wizard.setWizardContainer(ActuatorsPanel.this);
								JPanel panel = wizard.getWizardPanel();
								configurationPanel.add(panel);
							}
						}
						revalidate();
						repaint();
					}
				});

	}

	private Actuator getSelectedActuator() {
		int index = table.getSelectedRow();

		if (index == -1) {
			return null;
		}

		index = table.convertRowIndexToModel(index);
		return tableModel.getActuator(index);
	}

	private void search() {
		RowFilter<ActuatorsTableModel, Object> rf = null;
		// If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)"
					+ searchTextField.getText().trim());
		}
		catch (PatternSyntaxException e) {
			logger.warn("Search failed", e);
			return;
		}
		tableSorter.setRowFilter(rf);
	}

	@Override
	public void wizardCompleted(Wizard wizard) {
		configuration.setDirty(true);
	}

	@Override
	public void wizardCancelled(Wizard wizard) {
	}
}