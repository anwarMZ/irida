package ca.corefacility.bioinformatics.irida.repositories.filesystem;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PostLoad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import ca.corefacility.bioinformatics.irida.exceptions.StorageException;
import ca.corefacility.bioinformatics.irida.model.IridaThing;
import ca.corefacility.bioinformatics.irida.model.VersionedFileFields;

/**
 * Custom implementation of a repository that writes the {@link Path} part of an
 * entity to disk.
 * 
 *
 */
public abstract class FilesystemSupplementedRepositoryImpl<Type extends VersionedFileFields<Long> & IridaThing>
		implements FilesystemSupplementedRepository<Type> {

	private static final Logger logger = LoggerFactory.getLogger(FilesystemSupplementedRepository.class);

	private final Path baseDirectory;
	private final EntityManager entityManager;

	public FilesystemSupplementedRepositoryImpl(final EntityManager entityManager, final Path baseDirectory) {
		this.entityManager = entityManager;
		this.baseDirectory = baseDirectory;
	}
	
	public static class RelativePathTranslatorListener {
		
		private static final Map<Class<?>, Path> baseDirectories = new ConcurrentHashMap<>();
		
		private static final Predicate<Field> pathFilter = f -> f.getType().equals(Path.class);
		/**
		 * Get a collection of fields that have type Path.
		 * 
		 * @param type
		 *            the class type to get field references for.
		 * @return the set of field references for the class.
		 */
		private static Set<Field> findPathFields(final Class<?> type) {
			return Arrays.stream(type.getDeclaredFields()).filter(pathFilter).collect(Collectors.toSet());
		}
		
		public static void addBaseDirectory(final Class<?> c, final Path p) {
			baseDirectories.put(c, p);
		}
		
		@PostLoad
		public void absolutePath(final VersionedFileFields<Long> fileSystemEntity) {
			logger.debug("Going to get an absolute path after loading.");
			final Path directoryForType = baseDirectories.get(fileSystemEntity.getClass());
			// now find any members that are of type Path:
			final Set<Field> pathFields = findPathFields(fileSystemEntity.getClass());

			// for every member that's a path, make it an absolute path based on the
			// base directory
			for (final Field field : pathFields) {
				ReflectionUtils.makeAccessible(field);
				final Path source = (Path) ReflectionUtils.getField(field, fileSystemEntity);
				if (source != null) {
					
					final Path absolutePath = directoryForType.resolve(source);
					ReflectionUtils.setField(field, fileSystemEntity, absolutePath);
				}
			}
		}
	}

	/**
	 * Actually persist the entity to disk and to the database.
	 * 
	 * @param entity
	 *            the entity to persist.
	 * @return the persisted entity.
	 */
	protected Type saveInternal(final Type entity) {
		if (entity.getId() == null) {
			// save the initial version of the file to the database so that we
			// get an identifier attached to it.
			entityManager.persist(entity);
		}
		writeFilesToDisk(baseDirectory, entity);
		return entityManager.merge(entity);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Type updateWithoutFileRevision(Type entity) {
		return entityManager.merge(entity);
	}

	/**
	 * Persist an entity to disk and database. Implementors of this method are
	 * recommended to call
	 * {@link FilesystemSupplementedRepositoryImpl#saveInternal} to avoid
	 * repeated boilerplate code.
	 * 
	 * @param entity
	 *            the entity to persist.
	 * @return the persisted entity.
	 */
	public abstract Type save(final Type entity);

	/**
	 * Write any files to disk and update the {@link Path} location. This method
	 * works using reflection to automagically find and update any internal
	 * {@link Path} members on the {@link VersionedFileFields}. This class
	 * **does not** update the object in the database
	 * 
	 * @param baseDirectory
	 * @param iridaThing
	 */
	private Type writeFilesToDisk(Path baseDirectory, Type objectToWrite) {
		if (objectToWrite.getId() == null) {
			throw new IllegalArgumentException("Identifier is required.");
		}

		Path sequenceFileDir = baseDirectory.resolve(objectToWrite.getId().toString());

		Predicate<Field> pathFilter = f -> f.getType().equals(Path.class);
		// now find any members that are of type Path and shuffle them around:
		Set<Field> pathFields = Arrays.stream(objectToWrite.getClass().getDeclaredFields()).filter(pathFilter)
				.collect(Collectors.toSet());

		Set<Field> fieldsToUpdate = new HashSet<>();
		for (Field field : pathFields) {
			ReflectionUtils.makeAccessible(field);
			Path source = (Path) ReflectionUtils.getField(field, objectToWrite);
			if (source != null) {
				fieldsToUpdate.add(field);
			}
		}

		// if there are non-null fields, increment the revision number and
		// update the objects
		if (!fieldsToUpdate.isEmpty()) {
			objectToWrite.incrementFileRevisionNumber();
			Path sequenceFileDirWithRevision = sequenceFileDir
					.resolve(objectToWrite.getFileRevisionNumber().toString());

			for (Field field : fieldsToUpdate) {
				Path source = (Path) ReflectionUtils.getField(field, objectToWrite);
				Path target = sequenceFileDirWithRevision.resolve(source.getFileName());
				try {
					if (!Files.exists(sequenceFileDir)) {
						Files.createDirectory(sequenceFileDir);
						logger.trace("Created directory: [" + sequenceFileDir.toString() + "]");
					}

					if (!Files.exists(sequenceFileDirWithRevision)) {
						Files.createDirectory(sequenceFileDirWithRevision);
						logger.trace("Created directory: [" + sequenceFileDirWithRevision.toString() + "]");
					}

					Files.move(source, target);
					logger.trace("Moved file " + source + " to " + target);
				} catch (IOException e) {
					logger.error("Unable to move file into new directory", e);
					throw new StorageException("Failed to move file into new directory.", e);
				}
				
				final Path relativePath = baseDirectory.relativize(target);

				ReflectionUtils.setField(field, objectToWrite, relativePath);
			}
		}

		return objectToWrite;
	}
}
