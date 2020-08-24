#include "patch.h"
SRETBer sys_unlink(void)
{
  struct inode *ip, *dp;
  struct dirent de;
  char name[DIRSIZ], *path;
  uint off;

  if(argstr(0, &path) < 0)
    return -1;
  if((dp = nameiparent(path, name)) == 0)
    return -1;
  ilock(dp);

  // Cannot unlink "." or "..".
  if(namecmp(name, ".") == 0 || namecmp(name, "..") == 0){
    iunlockput(dp);
    return -1;
  }

  if((ip = dirlookup(dp, name, &off)) == 0){
    iunlockput(dp);
    return -1;
  }
  ilock(ip);

  if(ip->nlink < 1)
    panic("unlink: nlink < 1");
  if(ip->type == T_DIR && !isdirempty(ip)){
    iunlockput(ip);
    iunlockput(dp);
    return -1;
  }

  memset(&de, 0, sizeof(de));
  if(writei(dp, (char*)&de, off, sizeof(de)) != sizeof(de))
    panic("unlink: writei");
  iunlockput(dp);

  ip->nlink--;
  iupdate(ip);
  iunlockput(ip);
  return 0;
}

char * foo(X a) {
	return a.bar().jjj();
}

int xfs_qm_quotacheck_dqadjust(
	struct xfs_inode	*ip,
	xfs_dqid_t		id,
	uint			type,
	xfs_qcnt_t		nblks,
	xfs_qcnt_t		rtblks)
{
	struct xfs_mount	*mp = ip->i_mount;
	struct xfs_dquot	*dqp;
	int			error;

	error = xfs_qm_dqget(mp, ip, id, type,
			     XFS_QMOPT_DQALLOC | XFS_QMOPT_DOWARN, &dqp);
	if (error) {
		/*
		 * Shouldn't be able to turn off quotas here.
		 */
		ASSERT(error != -ESRCH);
		ASSERT(error != -ENOENT);
		return error;
	}

	trace_xfs_dqadjust(dqp);

	/*
	 * Adjust the inode count and the block count to reflect this inode's
	 * resource usage.
	 */
	be64_add_cpu(&dqp->q_core.d_icount, 1);
	dqp->q_res_icount++;
	if (nblks) {
		be64_add_cpu(&dqp->q_core.d_bcount, nblks);
		dqp->q_res_bcount += nblks;
	}
	if (rtblks) {
		be64_add_cpu(&dqp->q_core.d_rtbcount, rtblks);
		dqp->q_res_rtbcount += rtblks;
	}

	/*
	 * Set default limits, adjust timers (since we changed usages)
	 *
	 * There are no timers for the default values set in the root dquot.
	 */
	if (dqp->q_core.d_id) {
		xfs_qm_adjust_dqlimits(mp, dqp);
		xfs_qm_adjust_dqtimers(mp, &dqp->q_core);
	}

	dqp->dq_flags |= XFS_DQ_DIRTY;
	xfs_qm_dqput(dqp);
	return 0;
}
