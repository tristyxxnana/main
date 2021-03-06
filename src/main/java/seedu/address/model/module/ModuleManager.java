package seedu.address.model.module;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.StringUtil;
import seedu.address.model.StorageController;
import seedu.address.model.classroom.ClassroomManager;
import seedu.address.model.module.exceptions.DuplicateModuleException;
import seedu.address.model.person.Person;
import seedu.address.model.student.StudentManager;
import seedu.address.storage.adapter.XmlAdaptedModule;
import seedu.address.storage.adapter.XmlAdaptedStudentModule;
import seedu.address.ui.HtmlProcessor;
import seedu.address.ui.HtmlTableProcessor;

/**
 * This module manager stores modules for Trajectory.
 */
public class ModuleManager {

    private static final Logger logger = LogsCenter.getLogger(ModuleManager.class);

    private static ModuleManager instance;

    private ArrayList<Module> modules;

    private ModuleManager() {
        modules = new ArrayList<>();
        readModuleList();
    }

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    /**
     * Adds a new module to the in-memory array list
     */
    public void addModule(Module module) throws DuplicateModuleException {
        if (doesModuleExist(module)) {
            throw new DuplicateModuleException();
        }
        modules.add(module);
    }

    /**
     * Replaces the given module {@code target} with {@code editedModule}
     * {@code target} must already exist in Trajectory
     */
    public void updateModule(Module target, Module editedModule) {
        requireAllNonNull(target, editedModule);

        int targetIndex = modules.indexOf(target);

        modules.set(targetIndex, editedModule);
    }

    /**
     * Deletes a module from Trajectory. This method will also handle the deletion of the other components that
     * rely on {@code Module}.
     */
    public void deleteModule(Module toDelete) {
        modules.remove(toDelete);
        ClassroomManager.getInstance().handleModuleDeletedByModuleCode(toDelete.getModuleCode().moduleCode);
    }

    public void enrolStudentInModule(Module module, Person student) {
        module.addStudent(student);
    }

    public void removeStudentFromModule(Module module, Person student) {
        module.removeStudent(student);
    }

    public boolean isStudentEnrolledInModule(Module module, Person student) {
        return module.getEnrolledStudents().stream().anyMatch(s -> s.equals(student));
    }

    /**
     * Overload for {@link #isStudentEnrolledInModule(Module, Person)} to accept {@code String} inputs.
     * The module code and matric no. must be validated before calling this method, otherwise
     * there is a risk of a {@code NullPointerException} being thrown.
     */
    public boolean isStudentEnrolledInModule(String moduleCode, String matricNo) {
        Module module = getModuleByModuleCode(moduleCode);
        Person student = StudentManager.getInstance().retrieveStudentByMatricNo(matricNo);
        return isStudentEnrolledInModule(module, student);
    }

    /**
     * Removes a deleted student from the modules that student was previously enrolled in.
     */
    public void handleStudentDeleted(Person student) {
        for (Module module : modules) {
            if (isStudentEnrolledInModule(module, student)) {
                removeStudentFromModule(module, student);
            }
        }
    }

    /**
     * Gets the module list from storage and converts it to a Module array list.
     * Also reads the association data between Student and Module and stores it in-memory in the module.
     */
    private void readModuleList() {
        ArrayList<XmlAdaptedModule> xmlModuleList = StorageController.getModuleStorage();
        ArrayList<XmlAdaptedStudentModule> xmlAdaptedStudentModuleList = StorageController.getStudentModuleStorage();
        try {
            for (XmlAdaptedModule xmlModule : xmlModuleList) {
                Module m = xmlModule.toModelType();
                modules.add(m);

                // Look for associations between this module and any students (i.e. any enrolled students?)
                for (XmlAdaptedStudentModule xmlStudentModule : xmlAdaptedStudentModuleList) {
                    if (xmlStudentModule.getModuleCode().equals(m.getModuleCode().moduleCode)) {
                        Person student = StudentManager.getInstance()
                                .retrieveStudentByMatricNo(xmlStudentModule.getStudentMatricNo());

                        m.addStudent(student);
                    }
                }
            }
        } catch (NullPointerException npe) {
            logger.info("Illegal values found when reading enrolled students: " + npe.getMessage());
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found when reading module list: " + ive.getMessage());
        }
    }

    /**
     * Converts the Module array list and invokes the StorageController to save the current module list to file.
     * Also passes info on the association between Student and Module to be saved to file.
     */
    public void saveModuleList() {
        ArrayList<XmlAdaptedModule> xmlAdaptedModules =
                modules.stream().map(XmlAdaptedModule::new).collect(Collectors.toCollection(ArrayList::new));
        StorageController.setModuleStorage(xmlAdaptedModules);

        ArrayList<XmlAdaptedStudentModule> xmlAdaptedStudentModuleList = new ArrayList<>();
        for (Module m : modules) {
            xmlAdaptedStudentModuleList.addAll(
                    m.getEnrolledStudents()
                            .stream()
                            .map(s -> new XmlAdaptedStudentModule(
                                    s.getMatricNo().matricNo, m.getModuleCode().moduleCode))
                            .collect(Collectors.toCollection(ArrayList::new))
            );
        }
        StorageController.setStudentModuleStorage(xmlAdaptedStudentModuleList);

        StorageController.storeData();
    }

    /**
     * Searches the list of modules for module codes and/or names that match any of the keywords.
     * @return List of modules that match at least one keyword.
     */
    public List<Module> searchModulesWithKeywords(List<String> keywords) {
        return modules.stream()
                .filter(module ->
                        keywords.stream().anyMatch(keyword ->
                                StringUtil.containsWordIgnoreCase(module.getModuleCode().moduleCode, keyword)
                                || StringUtil.containsWordIgnoreCase(module.getModuleName().moduleName, keyword)))
                .collect(Collectors.toList());
    }

    /**
     * Searches the list of modules to find a module that matches the {@code moduleCode}
     * @param moduleCode The target module's code to find
     * @return The module object that matches the module code, or {@code null} if there isn't a matching module
     */
    public Module getModuleByModuleCode(String moduleCode) {
        return this.modules.stream()
                .filter(module -> module.getModuleCode().moduleCode.equals(moduleCode))
                .findAny()
                .orElse(null);
    }

    /**
     * Checks if the input module already exists in Trajectory.
     * @param module The module whose existence needs to be checked.
     * @return True if the module exists; false otherwise.
     */
    public boolean doesModuleExist(Module module) {
        return this.modules.stream().anyMatch(m -> m.isSameModule(module));
    }

    /**
     * Checks if the input module code matches a module that exists in Trajectory.
     * This is an overload to make it easier to check a module's existence without
     * creating a whole {@oode Module} object.
     * This overload adheres to the DRY principle by invoking the original
     * {@link #doesModuleExist(Module)} mathod.
     * @param moduleCode The module code that will be used to check for the module's existence.
     * @return True if the module exists; false otherwise.
     */
    public boolean doesModuleExist(String moduleCode) {
        Module module = getModuleByModuleCode(moduleCode);
        if (module != null) {
            return doesModuleExist(module);
        }
        return false;
    }

    /**
     * Converts the given list of modules to a HTML table representation.
     * @param moduleList
     * @return
     */
    public String convertModulesToTableRepresentation(List<Module> moduleList) {
        moduleList.sort(Comparator.comparing(m -> m.getModuleCode().toString()));

        StringBuilder sb = new StringBuilder();

        sb.append(HtmlTableProcessor.getBanner("Module List"));
        sb.append(HtmlTableProcessor.renderTableStart(new ArrayList<>(
                Arrays.asList("Module Code", "Module Name"))));

        sb.append(HtmlTableProcessor.getTableItemStart());
        for (Module m : moduleList) {
            ArrayList<String> dataRow = new ArrayList<>(Arrays.asList(
                    m.getModuleCode().toString(),
                    m.getModuleName().toString()
            ));

            sb.append(HtmlTableProcessor.renderTableItem(dataRow));
        }
        sb.append(HtmlTableProcessor.getTableItemEnd());

        return sb.toString();
    }

    /**
     * Converts the list of modules into a HTML table representation.
     * Internally calls {@link #convertModulesToTableRepresentation(List)} to generate the HTML string
     * to enforce DRY.
     * @return
     */
    public String getModuleTableRepresentation() {
        return convertModulesToTableRepresentation(modules);
    }

    /**
     * Converts the given module into a HTML String for display in the {@code BrowserPanel}
     */
    public String getModuleAsHtmlRepresentation(Module module) {
        final String listItemFormat = "%1$s (%2$s)";
        StringBuilder sb = new StringBuilder();
        StringBuilder studentEntries = new StringBuilder();

        // Sort the students by name in alphabetical order
        List<Person> enrolledStudents = new ArrayList<>(module.getEnrolledStudents());
        enrolledStudents.sort(Comparator.comparing(s -> s.getName().fullName));

        if (module.getEnrolledStudents().isEmpty()) {
            studentEntries.append("There are no students enrolled in this module.");
        } else {
            studentEntries.append(HtmlProcessor.getOrderedListStart());
            for (Person s : enrolledStudents) {
                studentEntries.append(HtmlProcessor.getListItem(
                        String.format(listItemFormat, s.getName().toString(), s.getMatricNo())
                ));
            }
            studentEntries.append(HtmlProcessor.getOrderedListEnd());
        }

        List<AbstractMap.SimpleEntry<String, String>> details = new ArrayList<>();
        details.add(new AbstractMap.SimpleEntry<>("Module Code: ", module.getModuleCode().toString()));
        details.add(new AbstractMap.SimpleEntry<>("Module Name: ", module.getModuleName().toString()));
        details.add(new AbstractMap.SimpleEntry<>("Enrolled Students: ", studentEntries.toString()));

        sb.append(HtmlProcessor.constructDetailedView(details));

        return sb.toString();
    }

    public void clearModules() {
        modules = new ArrayList<>();
    }

    public ArrayList<Module> getModules() {
        return this.modules;
    }
}
