import React from 'react'

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
  suffix?: React.ReactNode
}

export function Input({ label, error, hint, suffix, className = '', id, ...props }: InputProps) {
  const inputId = id ?? label

  return (
    <div className="flex flex-col gap-1">
      {label && (
        <label htmlFor={inputId} className="text-sm font-medium text-gray-700">
          {label}
          {props.required && <span className="ml-1 text-red-500">*</span>}
        </label>
      )}
      <div className="relative">
        <input
          id={inputId}
          className={[
            'w-full rounded-md border px-3 py-2 text-sm text-gray-900 placeholder-gray-400 outline-none transition-colors',
            'focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20',
            error
              ? 'border-red-400 focus:border-red-500 focus:ring-red-500/20'
              : 'border-gray-300',
            suffix ? 'pr-24' : '',
            className,
          ].join(' ')}
          {...props}
        />
        {suffix && (
          <div className="absolute inset-y-0 right-0 flex items-center">
            {suffix}
          </div>
        )}
      </div>
      {error && <p className="text-xs text-red-500">{error}</p>}
      {hint && !error && <p className="text-xs text-gray-400">{hint}</p>}
    </div>
  )
}
