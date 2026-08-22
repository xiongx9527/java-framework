export const isApplicationEnabled = (application) => application?.enable !== false

export const applicationEnableUpdate = (application) => ({
  enable: !isApplicationEnabled(application)
})
