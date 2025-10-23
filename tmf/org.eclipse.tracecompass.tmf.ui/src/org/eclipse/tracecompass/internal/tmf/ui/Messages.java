/*******************************************************************************
 * Copyright (c) 2012, 2018 Ericsson and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui;

import org.eclipse.osgi.util.NLS;

/**
 * TMF message bundle
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.tmf.ui.messages"; //$NON-NLS-1$

    public static String AddBookmarkDialog_Alpha;
    public static String AddBookmarkDialog_Color;

    public static String AddBookmarkDialog_Foreground;
    public static String AddBookmarkDialog_Message;
    public static String AddBookmarkDialog_Title;

    public static String ManageCustomParsersDialog_ConflictMessage;
    public static String ManageCustomParsersDialog_ConflictRenameButtonLabel;
    public static String ManageCustomParsersDialog_ConflictSkipButtonLabel;
    public static String ManageCustomParsersDialog_DeleteButtonLabel;
    public static String ManageCustomParsersDialog_DeleteConfirmation;
    public static String ManageCustomParsersDialog_DeleteParserDialogHeader;
    public static String ManageCustomParsersDialog_DialogHeader;
    public static String ManageCustomParsersDialog_EditButtonLabel;
    public static String ManageCustomParsersDialog_ExportButtonLabel;
    public static String ManageCustomParsersDialog_ExportParserSelection;
    public static String ManageCustomParsersDialog_ImportButtonLabel;
    public static String ManageCustomParsersDialog_ImportParserSelection;
    public static String ManageCustomParsersDialog_ImportFailureTitle;
    public static String ManageCustomParsersDialog_ImportFailureMessage;
    public static String ManageCustomParsersDialog_NewButtonLabel;
    public static String ManageCustomParsersDialog_TextButtonLabel;

    public static String MarkerEvent_Bookmarks;

    public static String TmfChartView_LockYAxis;
    public static String TmfChartViewer_NoData;
    public static String TmfCommonXLineChartTooltipProvider_time;

    public static String TmfEventsTable_AddAsFilterText;
    public static String TmfEventsTable_AddBookmarkActionText;
    public static String TmfEventsTable_ApplyPresetFilterMenuName;
    public static String TmfEventsTable_AutoFit;
    public static String TmfEventsTable_ClearFiltersActionText;
    public static String TmfEventsTable_CollapseFilterMenuName;
    public static String TmfEventsTable_ContentColumnHeader;
    public static String TmfEventsTable_CopyToClipboardActionText;
    public static String TmfEventsTable_Export_to_text;
    public static String TmfEventsTable_HideRawActionText;
    public static String TmfEventsTable_HideTableActionText;
    public static String TmfEventsTable_MultipleBookmarksToolTip;
    public static String TmfEventsTable_OpenModelActionText;
    public static String TmfEventsTable_OpenModelUnsupportedURI;
    public static String TmfEventsTable_ReferenceColumnHeader;
    public static String TmfEventsTable_RemoveBookmarkActionText;
    public static String TmfEventsTable_ResetAll;
    public static String TmfEventsTable_SearchHint;
    public static String TmfEventsTable_SearchingJobName;
    public static String TmfEventsTable_ShowAll;
    public static String TmfEventsTable_ShowRawActionText;
    public static String TmfEventsTable_ShowTableActionText;
    public static String TmfEventsTable_SourceColumnHeader;
    public static String TmfEventsTable_SynchronizeActionText;
    public static String TmfEventsTable_TimestampColumnHeader;
    public static String TmfEventsTable_TypeColumnHeader;

    public static String TmfMarker_LocationRank;
    public static String TmfMarker_LocationTime;
    public static String TmfMarker_LocationTimeRange;

    public static String TmfTimeFilterDialog_EDIT_PROFILING_OPTIONS;
    public static String TmfTimeFilterDialog_TRACE_FILTER;
    public static String TmfTimeFilterDialog_TRACE_FILTER_DESC;
    public static String TmfTimeFilterDialog_TRACE_ID;
    public static String TmfTimeFilterDialog_TRACE_NAME;
    public static String TmfTimeLegend_LEGEND;
    public static String TmfTimeLegend_TRACE_STATES;
    public static String TmfTimeLegend_TRACE_STATES_TITLE;
    public static String TmfTimeLegend_WINDOW_TITLE;
    public static String TmfTimeLegend_StateTypeName;
    public static String TmfTimeFilterDialog_WINDOW_TITLE;
    public static String TmfTimeFilterDialog_MESSAGE;
    public static String TmfTimeFilterDialog_CHECK_ALL;
    public static String TmfTimeFilterDialog_UNCHECK_ALL;
    public static String TmfTimeFilterDialog_CHECK_SELECTED;
    public static String TmfTimeFilterDialog_UNCHECK_SELECTED;
    public static String TmfTimeFilterDialog_CHECK_SUBTREE;
    public static String TmfTimeFilterDialog_UNCHECK_SUBTREE;

    public static String TmfTimeTipHandler_DURATION;
    public static String TmfTimeTipHandler_LINK_SOURCE;
    public static String TmfTimeTipHandler_LINK_SOURCE_TIME;
    public static String TmfTimeTipHandler_LINK_TARGET;
    public static String TmfTimeTipHandler_LINK_TARGET_TIME;
    public static String TmfTimeTipHandler_LINK_TIME;
    public static String TmfTimeTipHandler_PERCENT_OF_SELECTION;
    public static String TmfTimeTipHandler_TRACE_DATE;
    public static String TmfTimeTipHandler_TRACE_EVENT_TIME;
    public static String TmfTimeTipHandler_TRACE_START_TIME;
    public static String TmfTimeTipHandler_TRACE_STATE;
    public static String TmfTimeTipHandler_TRACE_STOP_TIME;

    public static String ShowFilterDialogAction_FilterActionNameText;
    public static String ShowFilterDialogAction_FilterActionToolTipText;

    public static String ShowFindDialogAction_Search;
    public static String ShowFindDialogAction_ShowSearchDialog;

    public static String TmfTimeGraphViewer_ResetScaleActionToolTipText;
    public static String TmfTimeGraphViewer_LegendActionNameText;
    public static String TmfTimeGraphViewer_LegendActionToolTipText;
    public static String TmfTimeGraphViewer_NextStateChangeActionNameText;
    public static String TmfTimeGraphViewer_NextStateChangeActionToolTipText;
    public static String TmfTimeGraphViewer_PreviousStateChangeActionNameText;
    public static String TmfTimeGraphViewer_PreviousStateChangeActionToolTipText;
    public static String TmfTimeGraphViewer_NextItemActionNameText;
    public static String TmfTimeGraphViewer_NextItemActionToolTipText;
    public static String TmfTimeGraphViewer_PreviousItemActionNameText;
    public static String TmfTimeGraphViewer_PreviousItemActionToolTipText;
    public static String TmfTimeGraphViewer_ZoomInActionNameText;
    public static String TmfTimeGraphViewer_ZoomInActionToolTipText;
    public static String TmfTimeGraphViewer_ZoomOutActionNameText;
    public static String TmfTimeGraphViewer_ZoomOutActionToolTipText;
    public static String TmfTimeGraphViewer_HideArrowsActionNameText;
    public static String TmfTimeGraphViewer_HideArrowsActionToolTipText;
    public static String TmfTimeGraphViewer_FollowArrowForwardActionNameText;
    public static String TmfTimeGraphViewer_FollowArrowForwardActionToolTipText;
    public static String TmfTimeGraphViewer_FollowArrowBackwardActionNameText;
    public static String TmfTimeGraphViewer_FollowArrowBackwardActionToolTipText;
    public static String TmfTimeGraphViewer_BookmarkActionAddText;
    public static String TmfTimeGraphViewer_BookmarkActionRemoveText;
    public static String TmfTimeGraphViewer_NextMarkerActionText;
    public static String TmfTimeGraphViewer_PreviousMarkerActionText;
    public static String TmfTimeGraphViewer_ShowGridlinesMenuText;
    public static String TmfTimeGraphViewer_ShowGridlinesHorizontalActionText;
    public static String TmfTimeGraphViewer_ShowGridlinesVerticalActionText;
    public static String TmfTimeGraphViewer_ShowMarkersMenuText;

    public static String Utils_ClockCyclesUnit;

    public static String ColorsView_AddActionToolTipText;
    public static String ColorsView_BackgroundButtonText;
    public static String ColorsView_BackgroundDialogText;
    public static String ColorsView_DeleteActionToolTipText;
    public static String ColorsView_ExportActionToolTipText;
    public static String ColorsView_FilterButtonText;
    public static String ColorsView_ForegroundButtonText;
    public static String ColorsView_ForegroundDialogText;
    public static String ColorsView_ImportActionToolTipText;
    public static String ColorsView_ImportOverwriteDialogMessage1;
    public static String ColorsView_ImportOverwriteDialogMessage2;
    public static String ColorsView_ImportOverwriteDialogTitle;
    public static String ColorsView_MoveDownActionToolTipText;
    public static String ColorsView_MoveUpActionToolTipText;
    public static String ColorsView_TickButtonText;
    public static String TickColorDialog_TickColorDialogTitle;

    public static String TimeGraphFindDialog_BackwardRadioButtonLabel;
    public static String TimeGraphFindDialog_CaseCheckBoxLabel;
    public static String TimeGraphFindDialog_CloseButtonLabel;
    public static String TimeGraphFindDialog_Direction;
    public static String TimeGraphFindDialog_FindLabel;
    public static String TimeGraphFindDialog_FindTitle;
    public static String TimeGraphFindDialog_FindNextButtonLabel;
    public static String TimeGraphFindDialog_ForwardRadioButtonLabel;
    public static String TimeGraphFindDialog_Options;
    public static String TimeGraphFindDialog_REgExCheckBoxLabel;
    public static String TimeGraphFindDialog_StatusNoMatchLabel;
    public static String TimeGraphFindDialog_StatusWrappedLabel;
    public static String TimeGraphFindDialog_WholeWordCheckBoxLabel;
    public static String TimeGraphFindDialog_WrapCheckBoxLabel;

    public static String TimeGraphLegend_Arrows;
    public static String TimeGraphLegend_resetTooltip;
    public static String TimeGraphLegend_swatchClick;
    public static String TimeGraphLegend_widthTooltip;

    public static String TimeGraphTooltipHandler_DefaultMarkerName;

    public static String TimeGraphTooltipHandler_Timestamp;

    public static String CustomTxtParserInputWizardPage_addChildLine;
    public static String CustomTxtParserInputWizardPage_addGroup;
    public static String CustomTxtParserInputWizardPage_addNextLine;
    public static String CustomTxtParserInputWizardPage_append;
    public static String CustomTxtParserInputWizardPage_appendWith;
    public static String CustomTxtParserInputWizardPage_capturedGroup;
    public static String CustomTxtParserInputWizardPage_cardinality;
    public static String CustomTxtParserInputWizardPage_category;
    public static String CustomTxtParserInputWizardPage_default;
    public static String CustomTxtParserInputWizardPage_desccriptionEdit;
    public static String CustomTxtParserInputWizardPage_descriptionNew;
    public static String CustomTxtParserInputWizardPage_eventType;
    public static String CustomTxtParserInputWizardPage_format;
    public static String CustomTxtParserInputWizardPage_group;
    public static String CustomTxtParserInputWizardPage_highlightAll;
    public static String CustomTxtParserInputWizardPage_logType;
    public static String CustomTxtParserInputWizardPage_matchingOtherLine;
    public static String CustomTxtParserInputWizardPage_matchingRootLine;
    public static String CustomTxtParserInputWizardPage_max;
    public static String CustomTxtParserInputWizardPage_min;
    public static String CustomTxtParserInputWizardPage_moveDown;
    public static String CustomTxtParserInputWizardPage_moveUp;
    public static String CustomTxtParserInputWizardPage_name;
    public static String CustomTxtParserInputWizardPage_newGroup;
    public static String CustomTxtParserInputWizardPage_noMatch;
    public static String CustomTxtParserInputWizardPage_noMatchingGroup;
    public static String CustomTxtParserInputWizardPage_noMatchingLine;
    public static String CustomTxtParserInputWizardPage_noMatchingTimestamp;
    public static String CustomTxtParserInputWizardPage_noMathcingLine;
    public static String CustomTxtParserInputWizardPage_nonMatchingLine;
    public static String CustomTxtParserInputWizardPage_noTimestampGroup;
    public static String CustomTxtParserInputWizardPage_preview;
    public static String CustomTxtParserInputWizardPage_previewInput;
    public static String CustomTxtParserInputWizardPage_previewLegend;
    public static String CustomTxtParserInputWizardPage_regularExpression;
    public static String CustomTxtParserInputWizardPage_regularExpressionHelp;
    public static String CustomTxtParserInputWizardPage_removeGroup;
    public static String CustomTxtParserInputWizardPage_removeLine;
    public static String CustomTxtParserInputWizardPage_set;
    public static String CustomTxtParserInputWizardPage_timestampFormat;
    public static String CustomTxtParserInputWizardPage_timestampFormatHelp;
    public static String CustomTxtParserInputWizardPage_uncapturedText;
    public static String CustomTxtParserInputWizardPage_unidentifiedCaptureGroup;
    public static String CustomTxtParserInputWizardPage_windowTitle;
    public static String CustomTxtParserInputWizardPage_titleEdit;
    public static String CustomTxtParserInputWizardPage_titleNew;
    public static String CustomParserOutputWizardPage_description;
    public static String CustomParserOutputWizardPage_moveAfter;
    public static String CustomParserOutputWizardPage_moveBefore;
    public static String CustomParserOutputWizardPage_visible;
    public static String CustomXmlParserInputWizardPage_default;
    public static String CustomXmlParserInputWizardPage_emptyCategoryError;
    public static String CustomXmlParserInputWizardPage_emptyEventTypeError;
    public static String CustomXmlParserInputWizardPage_emptyLogTypeError;
    public static String CustomXmlParserInputWizardPage_invalidCategoryError;
    public static String CustomXmlParserInputWizardPage_invalidLogTypeError;
    public static String CustomXmlParserInputWizardPage_duplicatelogTypeError;
    public static String CustomXmlParserInputWizardPage_noDocumentError;
    public static String CustomXmlParserInputWizardPage_missingLogEntryError;
    public static String CustomXmlParserInputWizardPage_missingDocumentElementError;
    public static String CustomXmlParserInputWizardPage_noTimestampElementOrAttribute;
    public static String CustomXmlParserInputWizardPage_elementMissingNameError;
    public static String CustomXmlParserInputWizardPage_elementMissingInputNameError;
    public static String CustomXmlParserInputWizardPage_elementMissingTimestampFmtError;
    public static String CustomXmlParserInputWizardPage_elementInvalidTimestampFmtError;
    public static String CustomXmlParserInputWizardPage_elementDuplicateNameError;
    public static String CustomXmlParserInputWizardPage_elementReservedInputNameError;
    public static String CustomXmlParserInputWizardPage_attributeMissingNameError;
    public static String CustomXmlParserInputWizardPage_attributeMissingInputNameError;
    public static String CustomXmlParserInputWizardPage_attributeMissingTimestampFmtError;
    public static String CustomXmlParserInputWizardPage_attributeInvalidTimestampFmtError;
    public static String CustomXmlParserInputWizardPage_attributeDuplicateNameError;
    public static String CustomXmlParserInputWizardPage_attributeReservedInputNameError;
    public static String CustomXmlParserInputWizardPage_addAttribute;
    public static String CustomXmlParserInputWizardPage_addChildElement;
    public static String CustomXmlParserInputWizardPage_addDocumentEleemnt;
    public static String CustomXmlParserInputWizardPage_addDocumentElement;
    public static String CustomXmlParserInputWizardPage_addNextElement;
    public static String CustomXmlParserInputWizardPage_append;
    public static String CustomXmlParserInputWizardPage_appendWith;
    public static String CustomXmlParserInputWizardPage_attibute;
    public static String CustomXmlParserInputWizardPage_category;
    public static String CustomXmlParserInputWizardPage_descriptionEdit;
    public static String CustomXmlParserInputWizardPage_descriptionNew;
    public static String CustomXmlParserInputWizardPage_elementName;
    public static String CustomXmlParserInputWizardPage_feelingLucky;
    public static String CustomXmlParserInputWizardPage_format;
    public static String CustomXmlParserInputWizardPage_logEntry;
    public static String CustomXmlParserInputWizardPage_logType;
    public static String CustomXmlParserInputWizardPage_moveDown;
    public static String CustomXmlParserInputWizardPage_moveUp;
    public static String CustomXmlParserInputWizardPage_name;
    public static String CustomXmlParserInputWizardPage_newAttibute;
    public static String CustomXmlParserInputWizardPage_noMatchingAttribute;
    public static String CustomXmlParserInputWizardPage_noMatch;
    public static String CustomXmlParserInputWizardPage_noMatchingElement;
    public static String CustomXmlParserInputWizardPage_preview;
    public static String CustomXmlParserInputWizardPage_previewInput;
    public static String CustomXmlParserInputWizardPage_removeAttribute;
    public static String CustomXmlParserInputWizardPage_removeElement;
    public static String CustomXmlParserInputWizardPage_set;
    public static String CustomXmlParserInputWizardPage_timestampFormat;
    public static String CustomXmlParserInputWizardPage_timestampFormatHelp;
    public static String CustomXmlParserInputWizardPage_titleEdit;
    public static String CustomXmlParserInputWizardPage_titleNew;
    public static String CustomXmlParserInputWizardPage_windowTitle;

    public static String FilterDialog_FilterDialogTitle;
    public static String FilterTreeLabelProvider_AspectHint;
    public static String FilterTreeLabelProvider_FilterNameHint;
    public static String FilterTreeLabelProvider_TraceTypeHint;
    public static String FilterTreeLabelProvider_ValueHint;
    public static String FilterView_AddActionToolTipText;
    public static String FilterView_DeleteActionToolTipText;
    public static String FilterView_ExportActionToolTipText;
    public static String FilterView_FileDialogFilterName;
    public static String FilterView_ImportActionToolTipText;
    public static String FilterView_SaveActionToolTipText;
    public static String FilterViewer_AlphaButtonText;
    public static String FilterViewer_AspectLabel;
    public static String FilterViewer_CommonCategory;
    public static String FilterViewer_DeleteActionText;
    public static String FilterViewer_EmptyTreeHintText;
    public static String FilterViewer_FieldHint;
    public static String FilterViewer_FieldLabel;
    public static String FilterViewer_FilterNameHint;
    public static String FilterViewer_IgnoreCaseButtonText;
    public static String FilterViewer_NameLabel;
    public static String FilterViewer_NewPrefix;
    public static String FilterViewer_NotLabel;
    public static String FilterViewer_NumButtonText;
    public static String FilterViewer_RegexHint;
    public static String FilterViewer_RegexLabel;
    public static String FilterViewer_ResultLabel;
    public static String FilterViewer_Subfield_ToolTip;
    public static String FilterViewer_TimestampButtonText;
    public static String FilterViewer_TypeLabel;
    public static String FilterViewer_ValueHint;
    public static String FilterViewer_ValueLabel;

    public static String TmfView_AlignViewsActionNameText;
    public static String TmfView_AlignViewsActionToolTipText;
    public static String TmfView_NewViewActionPinnedNewInstanceText;
    public static String TmfView_NewViewActionPinnedText;
    public static String TmfView_NewViewActionText;
    public static String TmfView_NewViewActionUnpinnedText;
    public static String TmfView_PinActionNameText;
    public static String TmfView_PinActionToolTipText;
    public static String TmfView_PinToActionText;
    public static String TmfView_UnpinActionText;
    public static String TmfView_ResetScaleActionNameText;

    public static String CopyToClipboardOperation_TaskName;
    public static String CopyToClipboardOperation_OutOfMemoryErrorTitle;
    public static String CopyToClipboardOperation_OutOfMemoryErrorMessage;

    public static String ExportToTextJob_Export_to;
    public static String ExportToTextJob_Export_trace_to;
    public static String ExportToTextJob_Unable_to_export_trace;

    public static String PerspectivesPreferencePage_SwitchToPerspectiveGroupText;
    public static String PerspectivesPreferencePage_SwitchToPerspectiveAlways;
    public static String PerspectivesPreferencePage_SwitchToPerspectiveNever;
    public static String PerspectivesPreferencePage_SwitchToPerspectivePrompt;

    public static String TmfPerspectiveManager_SwitchPerspectiveDialogMessage;
    public static String TmfPerspectiveManager_SwitchPerspectiveDialogTitle;
    public static String TmfPerspectiveManager_SwitchPerspectiveErrorMessage;
    public static String TmfPerspectiveManager_SwitchPerspectiveErrorTitle;

    public static String TmfTracingPreferencePage_TraceRangeInProjectExplorer;
    public static String TmfTracingPreferencePage_ConfirmDeletionSupplementaryFiles;
    public static String TmfTracingPreferencePage_AlwaysCloseOnResourceChange;
    public static String TmfTracingPreferencePage_HideManyEntriesSelectedWarning;
    public static String TmfTracingPreferencePage_UseBrowserTooltips;
    public static String TmfTracingPreferencePage_filterEmptyRowsTooltips;

    public static String TmfSourceLookup_OpenSourceCodeActionText;
    public static String TmfSourceLookup_OpenSourceCodeNotFound;
    public static String TmfSourceLookup_OpenSourceCodeSelectFileDialogTitle;

    public static String BaseDataProviderTimeGraphPresentationProvider_Trace;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
