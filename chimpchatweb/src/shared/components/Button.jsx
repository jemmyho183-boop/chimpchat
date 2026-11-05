export function Button({
  as = 'button',
  variant = 'primary',
  className = '',
  children,
  ...props
}) {
  const Component = as
  const classes = ['button', `button-${variant}`, className].filter(Boolean).join(' ')
  return <Component className={classes} {...props}>{children}</Component>
}
