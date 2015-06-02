/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 */
package de.fosd.jdime.merge;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeTriple;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.AddOperation;
import de.fosd.jdime.common.operations.ConflictOperation;
import de.fosd.jdime.common.operations.DeleteOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.NewMatching;
import org.apache.commons.lang3.ClassUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class OrderedMerge<T extends Artifact<T>> implements MergeInterface<T> {

	private static final Logger LOG = Logger.getLogger(ClassUtils.getShortClassName(OrderedMerge.class));
	private String logprefix;

	/**
	 * TODO: this needs high-level documentation. Probably also detailed documentation.
	 *
	 * @param operation
	 * @param context
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Override
	public final void merge(final MergeOperation<T> operation,
			final MergeContext context) throws IOException,
			InterruptedException {
		boolean logFinest = LOG.isLoggable(Level.FINEST);

		MergeTriple<T> triple = operation.getMergeTriple();
		T left = triple.getLeft();
		T base = triple.getBase();
		T right = triple.getRight();
		T target = operation.getTarget();
		logprefix = operation.getId() + " - ";

		assert (left.matches(right));
		assert (left.hasMatching(right)) && right.hasMatching(left);

		LOG.finest(() -> {
			String name = getClass().getSimpleName();
			return String.format("%s%s.merge(%s, %s, %s)", prefix(), name, left.getId(), base.getId(), right.getId());
		});

		Revision l = left.getRevision();
		Revision b = base.getRevision();
		Revision r = right.getRevision();
		Iterator<T> leftIt = left.getChildren().iterator();
		Iterator<T> rightIt = right.getChildren().iterator();

		boolean leftdone = false;
		boolean rightdone = false;
		T leftChild = null;
		T rightChild = null;

		if (leftIt.hasNext()) {
			leftChild = leftIt.next();
		} else {
			leftdone = true;
		}
		if (rightIt.hasNext()) {
			rightChild = rightIt.next();
		} else {
			rightdone = true;
		}

		while (!leftdone || !rightdone) {
			if (!leftdone && !r.contains(leftChild)) {
				assert (leftChild != null);
				if (logFinest) LOG.finest(String.format("%s is not in right", prefix(leftChild)));

				if (b != null && b.contains(leftChild)) {
					if (logFinest) LOG.finest(String.format("%s was deleted by right", prefix(leftChild)));

					// was deleted in right
					if (leftChild.hasChanges()) {
						// insertion-deletion-conflict
						if (logFinest) LOG.finest(String.format("%s has changes in subtree", prefix(leftChild)));

						ConflictOperation<T> conflictOp = new ConflictOperation<>(
								leftChild, leftChild, rightChild, target);
						conflictOp.apply(context);
						if (rightIt.hasNext()) {
							rightChild = rightIt.next();
						} else {
							rightdone = true;
						}
						if (leftIt.hasNext()) {
							leftChild = leftIt.next();
						} else {
							leftdone = true;
						}
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<>(
								leftChild);
						delOp.apply(context);
					}
				} else {
					if (logFinest) LOG.finest(String.format("%s is a change", prefix(leftChild)));

					// leftChild is a change
					if (!rightdone && !l.contains(rightChild)) {
						assert (rightChild != null);
						if (logFinest) LOG.finest(String.format("%s is not in left", prefix(rightChild)));

						if (b != null && b.contains(rightChild)) {
							if (logFinest) LOG.finest(String.format("%s was deleted by left", prefix(rightChild)));

							// rightChild was deleted in left
							if (rightChild.hasChanges()) {
								if (logFinest) LOG.finest(String.format("%s has changes in subtree.", prefix(rightChild)));

								// deletion-insertion conflict
								ConflictOperation<T> conflictOp = new ConflictOperation<>(
										rightChild, leftChild, rightChild, target);
								conflictOp.apply(context);
								if (rightIt.hasNext()) {
									rightChild = rightIt.next();
								} else {
									rightdone = true;
								}
								if (leftIt.hasNext()) {
									leftChild = leftIt.next();
								} else {
									leftdone = true;
								}
							} else {
								// add the left change
								AddOperation<T> addOp = new AddOperation<>(
										leftChild, target);
								leftChild.setMerged(true);
								addOp.apply(context);
							}
						} else {
							if (logFinest) LOG.finest(String.format("%s is a change", prefix(rightChild)));

							// rightChild is a change
							ConflictOperation<T> conflictOp = new ConflictOperation<>(
									leftChild, leftChild, rightChild, target);
							conflictOp.apply(context);

							if (rightIt.hasNext()) {
								rightChild = rightIt.next();
							} else {
								rightdone = true;
							}
						}
					} else {
						if (logFinest) LOG.finest(String.format("%s adding change", prefix(leftChild)));

						// add the left change
						AddOperation<T> addOp = new AddOperation<>(leftChild,
								target);
						leftChild.setMerged(true);
						addOp.apply(context);
					}
				}

				if (leftIt.hasNext()) {
					leftChild = leftIt.next();
				} else {
					leftdone = true;
				}
			}

			if (!rightdone && !l.contains(rightChild)) {
				assert (rightChild != null);
				if (logFinest) LOG.finest(String.format("%s is not in left", prefix(rightChild)));

				if (b != null && b.contains(rightChild)) {
					if (logFinest) LOG.finest(String.format("%s was deleted by left", prefix(rightChild)));

					// was deleted in left
					if (rightChild.hasChanges()) {
						if (logFinest) LOG.finest(String.format("%s has changes in subtree.", prefix(rightChild)));

						// insertion-deletion-conflict
						ConflictOperation<T> conflictOp = new ConflictOperation<>(
								rightChild, leftChild, rightChild, target);
						conflictOp.apply(context);
						if (rightIt.hasNext()) {
							rightChild = rightIt.next();
						} else {
							rightdone = true;
						}
						if (leftIt.hasNext()) {
							leftChild = leftIt.next();
						} else {
							leftdone = true;
						}
					} else {
						// can be safely deleted
						DeleteOperation<T> delOp = new DeleteOperation<>(
								rightChild);
						delOp.apply(context);
					}
				} else {
					if (logFinest) LOG.finest(String.format("%s is a change", prefix(rightChild)));

					// rightChild is a change
					if (!leftdone && !r.contains(leftChild)) {
						assert (leftChild != null);
						if (logFinest) LOG.finest(String.format("%s is not in right", prefix(leftChild)));

						if (b != null && b.contains(leftChild)) {
							if (logFinest) LOG.finest(String.format("%s was deleted by right", prefix(leftChild)));

							if (leftChild.hasChanges()) {
								if (logFinest) LOG.finest(String.format("%s has changes in subtree", prefix(leftChild)));

								// deletion-insertion conflict
								ConflictOperation<T> conflictOp = new ConflictOperation<>(
										leftChild, leftChild, rightChild, target);
								conflictOp.apply(context);
								if (rightIt.hasNext()) {
									rightChild = rightIt.next();
								} else {
									rightdone = true;
								}
								if (leftIt.hasNext()) {
									leftChild = leftIt.next();
								} else {
									leftdone = true;
								}
							} else {
								if (logFinest) LOG.finest(String.format("%s adding change", prefix(rightChild)));

								// add the right change
								AddOperation<T> addOp = new AddOperation<>(
										rightChild, target);
								rightChild.setMerged(true);
								addOp.apply(context);
							}
						} else {
							if (logFinest) LOG.finest(String.format("%s is a change", prefix(leftChild)));

							// leftChild is a change
							ConflictOperation<T> conflictOp = new ConflictOperation<>(
									leftChild, leftChild, rightChild, target);
							conflictOp.apply(context);

							if (leftIt.hasNext()) {
								leftChild = leftIt.next();
							} else {
								leftdone = true;
							}
						}
					} else {
						if (logFinest) LOG.finest(String.format("%s adding change", prefix(rightChild)));

						// add the right change
						AddOperation<T> addOp = new AddOperation<>(rightChild,
								target);
						rightChild.setMerged(true);
						addOp.apply(context);
					}
				}

				if (rightIt.hasNext()) {
					rightChild = rightIt.next();
				} else {
					rightdone = true;
				}

			} else if (l.contains(rightChild) && r.contains(leftChild)) {
				assert (leftChild != null);
				assert (rightChild != null);

				// left and right have the artifact. merge it.
				if (logFinest)
					LOG.finest(String.format("%s is in both revisions [%s]", prefix(leftChild), rightChild.getId()));

				assert (leftChild.hasMatching(rightChild) && rightChild
						.hasMatching(leftChild));

				if (!leftChild.isMerged() && !rightChild.isMerged()) {
					// determine whether the child is 2 or 3-way merged
					NewMatching<T> mBase = leftChild.getMatching(b);

					MergeType childType = mBase == null ? MergeType.TWOWAY
							: MergeType.THREEWAY;
					T baseChild = mBase == null ? leftChild.createEmptyDummy()
							: mBase.getMatchingArtifact(leftChild);
					T targetChild = target == null ? null : target
							.addChild(leftChild);

					MergeTriple<T> childTriple = new MergeTriple<>(childType,
							leftChild, baseChild, rightChild);

					MergeOperation<T> mergeOp = new MergeOperation<>(
							childTriple, targetChild);

					leftChild.setMerged(true);
					rightChild.setMerged(true);
					mergeOp.apply(context);
				}

				if (leftIt.hasNext()) {
					leftChild = leftIt.next();
				} else {
					leftdone = true;
				}

				if (rightIt.hasNext()) {
					rightChild = rightIt.next();
				} else {
					rightdone = true;
				}
			}
			if (logFinest && target != null) {
				LOG.finest(String.format("%s target.dumpTree() after processing child:", prefix()));
				System.out.println(target.dumpRootTree());
			}
		}
	}

	/**
	 * Returns the logging prefix.
	 *
	 * @return logging prefix
	 */
	private String prefix() {
		return logprefix;
	}

	/**
	 * Returns the logging prefix.
	 *
	 * @param artifact
	 *            artifact that is subject of the logging
	 * @return logging prefix
	 */
	private String prefix(final T artifact) {
		return String.format("%s[%s] ", logprefix, artifact == null ? "null" : artifact.getId());
	}
}
