package seedu.address.logic.commands;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import seedu.address.commons.util.CsvUtil;
import seedu.address.logic.CommandHistory;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ModelManager;
import seedu.address.model.StorageController;
import seedu.address.model.note.NoteManager;
import seedu.address.testutil.NoteBuilder;

/**
 * Contains tests for NoteExportCommand.
 */
public class NoteExportCommandTest {

    private static NoteManager noteManager = NoteManager.getInstance();

    private NoteBuilder note1 = new NoteBuilder();

    @Before
    public void setUp() {
        StorageController.enterTestMode();
        noteManager.clearNotes();
        noteManager.saveNoteList();
    }

    @Test
    public void execute_containsExportableNotes_success() throws CommandException {
        String expectedMessage = NoteExportCommand.MESSAGE_SUCCESS;

        noteManager.addNote(new NoteBuilder().build());
        noteManager.addNote(new NoteBuilder().build());
        noteManager.addNote(new NoteBuilder().build());
        noteManager.saveNoteList(); // three exportable notes

        String fileName = "valid_file_name";
        NoteExportCommand noteExportCommand = new NoteExportCommand(fileName);
        CommandResult result = noteExportCommand.execute(new ModelManager(), new CommandHistory());

        assertEquals(
                String.format(expectedMessage, 3, CsvUtil.BASE_DIRECTORY + fileName + ".csv"),
                result.feedbackToUser);
    }

    @Test
    public void execute_noExportableNotes_displaysMessageNoExportableNotes() throws CommandException {
        String expectedMessage = NoteExportCommand.MESSAGE_NO_EXPORTABLE_NOTES;

        // setup notes with null start dates
        NoteBuilder noteA = new NoteBuilder(note1.build());
        NoteBuilder noteB = new NoteBuilder(note1.build());
        NoteBuilder noteC = new NoteBuilder(note1.build());

        noteA = noteA.withNullStartDate();
        noteB = noteB.withNullStartDate();
        noteC = noteC.withNullStartDate();

        noteManager.addNote(noteA.build());
        noteManager.addNote(noteB.build());
        noteManager.addNote(noteC.build());
        noteManager.saveNoteList(); // three notes with null start date, zero exportable notes

        NoteExportCommand noteExportCommand = new NoteExportCommand("valid_file_name");
        CommandResult result = noteExportCommand.execute(new ModelManager(), new CommandHistory());

        assertEquals(expectedMessage, result.feedbackToUser);
    }

    @AfterClass
    public static void tearDown() {
        noteManager.clearNotes();
        noteManager.saveNoteList();
    }
}
